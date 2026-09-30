package com.meetgrid.config;
import com.meetgrid.model.Account;
import com.meetgrid.repository.AccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class AdminBootstrap implements ApplicationRunner {
 private final Environment env;private final AccountRepository accounts;private final PasswordEncoder passwords;
 public AdminBootstrap(Environment e,AccountRepository a,PasswordEncoder p){env=e;accounts=a;passwords=p;}
 public String email(){return env.getProperty("ADMIN_EMAIL","vivekni1224@nigam").strip().toLowerCase(Locale.ROOT);}
 @Override public void run(ApplicationArguments args){
  String password=env.getProperty("ADMIN_BOOTSTRAP_PASSWORD","");if(password.isBlank())return;
  if(password.length()<16||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD must contain 16–72 UTF-8 bytes.");
  var existing=accounts.findByEmail(email());
  if(existing.isPresent()){
   var account=existing.get();if(account.role.equals("ADMIN"))return;
   if(!passwords.matches(password,account.passwordHash))throw new IllegalStateException("Admin identifier already belongs to an account. Supply that account's password to authorize promotion.");
   account.role="ADMIN";account.suspended=false;accounts.save(account);return;
  }
  var a=new Account();a.id=UUID.randomUUID().toString();a.email=email();a.passwordHash=passwords.encode(password);
  a.displayName="Administrator";a.workspaceName="Administration";a.timezone="Asia/Kolkata";a.role="ADMIN";accounts.save(a);
 }
}
