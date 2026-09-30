package com.meetgrid.service;
import com.meetgrid.model.Account;
import com.meetgrid.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.sql.Timestamp;
import java.security.SecureRandom;
import java.util.*;
@Service
public class AccountSecurity {
 private final AccountRepository accounts;private final JdbcTemplate db;private final MailOutbox mail;private final RateLimits limits;private final PasswordEncoder encoder;private final Environment env;
 public AccountSecurity(AccountRepository a,JdbcTemplate d,MailOutbox m,RateLimits r,PasswordEncoder p,Environment e){accounts=a;db=d;mail=m;limits=r;encoder=p;env=e;}
 public void ready(){mail.requireConfigured();}
 @Transactional public void issue(Account account,String purpose){
  mail.requireConfigured();var a=accounts.lockById(account.id).orElseThrow();
  limits.check("email",a.email,env.getProperty("meetgrid.limits.emails-per-hour",Integer.class,5),3600);
  db.update("UPDATE auth_tokens SET consumed=TRUE WHERE account_id=? AND purpose=?",a.id,purpose);
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);Instant expiry=Instant.now().plusSeconds(purpose.equals("RESET")?1200:86400);
  db.update("INSERT INTO auth_tokens(token_hash,account_id,purpose,expires_at,consumed) VALUES(?,?,?,?,FALSE)",RateLimits.hash(token),a.id,purpose,Timestamp.from(expiry));
  mail.queue(a.email,a.displayName,purpose,token,expiry);
 }
 @Transactional public void forgot(String email){ready();limits.check("recovery",email,5,3600);accounts.findByEmail(email).filter(a->!a.suspended).ifPresent(a->issue(a,"RESET"));}
 @Transactional public void consume(String token,String purpose,String password){
  var matches=db.queryForList("SELECT account_id FROM auth_tokens WHERE token_hash=?",RateLimits.hash(token));if(matches.isEmpty())throw invalid();
  var account=accounts.lockById((String)matches.getFirst().get("account_id")).orElseThrow(this::invalid);if(account.suspended)throw invalid();
  int used=db.update("UPDATE auth_tokens SET consumed=TRUE WHERE token_hash=? AND account_id=? AND purpose=? AND consumed=FALSE AND expires_at>CURRENT_TIMESTAMP",RateLimits.hash(token),account.id,purpose);if(used!=1)throw invalid();
  if(purpose.equals("RESET")){validatePassword(password);account.passwordHash=encoder.encode(password);account.hasPassword=true;account.authVersion++;db.update("UPDATE auth_tokens SET consumed=TRUE WHERE account_id=?",account.id);}
  account.emailVerified=true;accounts.save(account);
 }
 @Transactional public void change(String id,String oldPassword,String password){var a=accounts.lockById(id).orElseThrow();if(!a.hasPassword||!encoder.matches(oldPassword,a.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect.");validatePassword(password);a.passwordHash=encoder.encode(password);a.authVersion++;accounts.save(a);db.update("UPDATE auth_tokens SET consumed=TRUE WHERE account_id=?",id);}
 public static void validatePassword(String password){if(password==null||password.length()<12||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a password with at least 12 characters and at most 72 UTF-8 bytes.");}
 private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"This link is invalid, expired, or already used. Request a new email.");}
}
