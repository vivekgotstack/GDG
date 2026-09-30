package com.meetgrid.config;
import com.meetgrid.model.Account;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
@Component
public class SessionLogin {
 private final SecurityContextRepository repository;public SessionLogin(SecurityContextRepository r){repository=r;}
 public void authenticate(Account a,HttpServletRequest req,HttpServletResponse res){if(req.getSession(false)!=null)req.getSession(false).invalidate();req.getSession(true).setAttribute("AUTH_VERSION",a.authVersion);var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(a.id,null,AuthorityUtils.createAuthorityList("ROLE_USER","ROLE_"+a.role)));SecurityContextHolder.setContext(context);repository.saveContext(context,req,res);}
}
