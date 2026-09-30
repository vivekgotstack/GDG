package com.meetgrid.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.sql.Timestamp;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
@Service
public class MailOutbox {
 private final JdbcTemplate db;private final Environment env;private final ObjectMapper json;private final TransactionTemplate tx;private final RestClient client;private final String template;
 public MailOutbox(JdbcTemplate db,Environment env,ObjectMapper json,TransactionTemplate tx)throws java.io.IOException{
  this.db=db;this.env=env;this.json=json;this.tx=tx;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));client=RestClient.builder().baseUrl("https://api.brevo.com/v3").requestFactory(factory).build();
  template=new String(new ClassPathResource("mail/auth-email.html").getInputStream().readAllBytes(),StandardCharsets.UTF_8);
 }
 public boolean configured(){try{return !setting("api-key").isBlank()&&!setting("api-key").startsWith("REPLACE_")&&Base64.getDecoder().decode(setting("encryption-key")).length==32;}catch(Exception e){return false;}}
 public void requireConfigured(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is not configured yet. Please contact support.");}
 private String setting(String name){return env.getProperty("meetgrid.mail."+name,"");}
 public void queue(String recipient,String name,String purpose,String token,Instant expires){
  requireConfigured();String origin=env.getProperty("meetgrid.app-url");var uri=URI.create(origin);if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null)throw new IllegalStateException("APP_URL must be a valid web origin");
  boolean reset=purpose.equals("RESET");String title=reset?"A fresh start for your account.":"Your people. Your space. Your email.";
  String subject=reset?"Reset your MeetGrid password":"Verify your MeetGrid email";
  String message=reset?"Use the button below to choose a new password. This link expires in 20 minutes. If you did not request this, you can ignore this email.":"Confirm this email to open your workspace and start finding common ground. This link expires in 24 hours.";
  String url=origin.replaceAll("/+$","")+(reset?"/reset-password":"/verify-email")+"#token="+token;
  String action=reset?"Choose a new password":"Verify my email";
  String html=template.replace("{{name}}",HtmlUtils.htmlEscape(name)).replace("{{title}}",title).replace("{{message}}",message).replace("{{action}}",action).replace("{{url}}",HtmlUtils.htmlEscape(url)).replace("{{support}}",HtmlUtils.htmlEscape(setting("reply-to")));
  try{String payload=json.writeValueAsString(Map.of("to",List.of(Map.of("email",recipient)),"sender",Map.of("email",setting("sender-email"),"name",setting("sender-name")),"replyTo",Map.of("email",setting("reply-to")),"subject",subject,"htmlContent",html,"textContent","Hi "+name+",\n\n"+message+"\n\n"+action+": "+url+"\n\nMeetGrid · StackOrcs"));
   db.update("INSERT INTO mail_outbox(id,encrypted_payload,state,attempts,next_attempt,expires_at,created_at) VALUES(?,?,'PENDING',0,?,?,?)",UUID.randomUUID().toString(),encrypt(payload),Timestamp.from(Instant.now()),Timestamp.from(expires),Timestamp.from(Instant.now()));
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new IllegalStateException("Could not queue account email",e);}
 }
 private String encrypt(String plain)throws Exception{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(Base64.getDecoder().decode(setting("encryption-key")),"AES"),new GCMParameterSpec(128,iv));return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));}
 private String decrypt(String value)throws Exception{String[] parts=value.split(":",2);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(Base64.getDecoder().decode(setting("encryption-key")),"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}
 @Scheduled(fixedDelayString="${MAIL_POLL_INTERVAL_MS:15000}")
 public void deliver(){if(!configured())return;for(int i=0;i<5;i++){Boolean found=tx.execute(status->{
   var rows=db.queryForList("SELECT *,expires_at<=CURRENT_TIMESTAMP AS expired FROM mail_outbox WHERE state='PENDING' AND next_attempt<=CURRENT_TIMESTAMP ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED");if(rows.isEmpty())return false;
   var row=rows.getFirst();String id=(String)row.get("id");
   if(Boolean.TRUE.equals(row.get("expired"))){db.update("UPDATE mail_outbox SET state='EXPIRED',encrypted_payload='' WHERE id=?",id);return true;}
   int attempt=((Number)row.get("attempts")).intValue()+1;
   try{client.post().uri("/smtp/email").header("api-key",setting("api-key")).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(decrypt((String)row.get("encrypted_payload"))).retrieve().toBodilessEntity();db.update("UPDATE mail_outbox SET state='SENT',encrypted_payload='',attempts=?,last_error=NULL WHERE id=?",attempt,id);}
   catch(Exception e){db.update("UPDATE mail_outbox SET state=?,attempts=?,next_attempt=?,last_error=? WHERE id=?",attempt>=6?"FAILED":"PENDING",attempt,Timestamp.from(Instant.now().plusSeconds(Math.min(900,15L<<attempt))),e instanceof org.springframework.web.client.RestClientResponseException re?"Provider HTTP "+re.getStatusCode().value():"Delivery unavailable",id);}
   return true;});if(!Boolean.TRUE.equals(found))break;}}
 @Scheduled(fixedDelay=3600000) public void cleanup(){db.update("DELETE FROM auth_tokens WHERE expires_at<CURRENT_TIMESTAMP - INTERVAL '1 day'");db.update("DELETE FROM mail_outbox WHERE expires_at<CURRENT_TIMESTAMP - INTERVAL '7 days'");}
}
