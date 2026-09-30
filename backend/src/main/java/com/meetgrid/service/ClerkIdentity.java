package com.meetgrid.service;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
@Service
public class ClerkIdentity {
 public record Identity(String subject,String email,String name){}
 private final Environment env;private final RestClient api;private volatile NimbusJwtDecoder decoder;
 public ClerkIdentity(Environment env){this.env=env;var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(8));api=RestClient.builder().baseUrl("https://api.clerk.com/v1").requestFactory(factory).build();}
 public Identity verify(String token){String secret=env.getProperty("meetgrid.clerk.secret-key","");String issuer=env.getProperty("meetgrid.clerk.issuer","").replaceAll("/+$","");if(secret.startsWith("REPLACE_")||secret.isBlank()||issuer.contains("REPLACE_"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Social sign-in is not configured yet.");
  try{if(decoder==null){synchronized(this){if(decoder==null){var d=NimbusJwtDecoder.withJwkSetUri(issuer+"/.well-known/jwks.json").build();d.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));decoder=d;}}}
   var jwt=decoder.decode(token);String origin=env.getProperty("meetgrid.app-url","").replaceAll("/+$","");
   if(jwt.getExpiresAt()==null||!origin.equals(jwt.getClaimAsString("azp"))||jwt.getSubject()==null||!jwt.getSubject().matches("user_[a-zA-Z0-9]+")||"pending".equals(jwt.getClaimAsString("sts")))throw new IllegalArgumentException();
   JsonNode user=api.get().uri("/users/{id}",jwt.getSubject()).header("Authorization","Bearer "+secret).retrieve().body(JsonNode.class);
   if(user==null||user.path("banned").asBoolean()||user.path("locked").asBoolean())throw new IllegalArgumentException();String primary=user.path("primary_email_address_id").asText();
   for(var email:user.path("email_addresses")){if(email.path("id").asText().equals(primary)&&email.path("verification").path("status").asText().equals("verified")){String name=(user.path("first_name").asText("")+" "+user.path("last_name").asText("")).strip();return new Identity(jwt.getSubject(),email.path("email_address").asText().toLowerCase(Locale.ROOT),name.isBlank()?"Workspace owner":name.substring(0,Math.min(name.length(),80)));}}
   throw new IllegalArgumentException();
  }catch(org.springframework.web.client.RestClientException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Social sign-in provider is temporarily unavailable.");}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Social sign-in could not be verified. Use a verified email and try again.");}
 }
}
