package com.meetgrid.controller;
import com.meetgrid.config.*;
import com.meetgrid.model.Account;
import com.meetgrid.repository.AccountRepository;
import com.meetgrid.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/auth")
public class AccountSecurityController {
 private final AccountSecurity security;private final AccountRepository accounts;private final ClerkIdentity clerk;private final SessionLogin login;private final PasswordEncoder encoder;private final AdminBootstrap admin;
 public AccountSecurityController(AccountSecurity s,AccountRepository a,ClerkIdentity c,SessionLogin l,PasswordEncoder p,AdminBootstrap b){security=s;accounts=a;clerk=c;login=l;encoder=p;admin=b;}
 public record Email(@NotBlank @jakarta.validation.constraints.Email @Size(max=254) String email){}
 public record Token(@NotBlank @Size(max=100) String token){}
 public record Reset(@NotBlank @Size(max=100) String token,@NotBlank @Size(min=12,max=72) String password){}
 public record Change(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(min=12,max=72) String password){}
 public record Social(@NotBlank @Size(max=16000) String token){}
 @PostMapping("/forgot-password") public Map<String,String> forgot(@Valid @RequestBody Email e){security.forgot(e.email().strip().toLowerCase(Locale.ROOT));return Map.of("message","If that address can receive account emails, a reset link will arrive shortly.");}
 @PostMapping("/reset-password") public Map<String,String> reset(@Valid @RequestBody Reset r){security.consume(r.token(),"RESET",r.password());return Map.of("message","Password updated. Sign in again on your devices.");}
 @PostMapping("/verify-email") public Map<String,String> verify(@Valid @RequestBody Token t){security.consume(t.token(),"VERIFY",null);return Map.of("message","Email verified. Your workspace is ready.");}
 @PostMapping("/resend-verification") public Map<String,String> resend(){var a=accounts.findById(WorkspaceIdentity.id()).orElseThrow();if(!a.emailVerified)security.issue(a,"VERIFY");return Map.of("message","Check your inbox for a verification link. It may take a moment to arrive.");}
 @PostMapping("/change-password") public Map<String,String> change(@Valid @RequestBody Change c,HttpServletRequest req){security.change(WorkspaceIdentity.id(),c.currentPassword(),c.password());if(req.getSession(false)!=null)req.getSession(false).invalidate();SecurityContextHolder.clearContext();return Map.of("message","Password changed. All sessions have been signed out.");}
 @PostMapping("/clerk") @Transactional public AuthController.UserView social(@Valid @RequestBody Social input,HttpServletRequest req,HttpServletResponse res){
  var identity=clerk.verify(input.token());Account a=accounts.findByClerkSubject(identity.subject()).orElse(null);
  if(a==null){var existing=accounts.findByEmail(identity.email());if(existing.isPresent()){
    if(!existing.get().id.equals(WorkspaceIdentity.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This email already has a workspace. Sign in with its password, then link social sign-in from Settings.");
    a=accounts.lockById(existing.get().id).orElseThrow();if(a.clerkSubject!=null&&!a.clerkSubject.equals(identity.subject()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Another social identity is already linked.");
   }else{if(identity.email().equals(admin.email()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sign in with the administrator password first.");a=new Account();a.id=UUID.randomUUID().toString();a.email=identity.email();a.displayName=identity.name();a.workspaceName=identity.name()+" workspace";a.timezone="UTC";a.passwordHash=encoder.encode(UUID.randomUUID().toString());a.hasPassword=false;}
   a.clerkSubject=identity.subject();a.emailVerified=true;
  }
  if(a.suspended)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This account is unavailable.");
  accounts.saveAndFlush(a);login.authenticate(a,req,res);return AuthController.UserView.from(a);
 }
}
