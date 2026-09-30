package com.meetgrid.config;
import com.meetgrid.repository.AccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import java.io.IOException;

// Database checks make suspension and role changes effective for existing sessions.
public class AccountAccessFilter extends OncePerRequestFilter {
 private final AccountRepository accounts;
 public AccountAccessFilter(AccountRepository accounts){this.accounts=accounts;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)){
   var account=accounts.findById(auth.getName()).orElse(null);
   if(account==null||account.suspended){
    SecurityContextHolder.clearContext();if(req.getSession(false)!=null)req.getSession(false).invalidate();
    res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"This account is unavailable. Contact support for help.\"}");return;
   }
   var context=SecurityContextHolder.createEmptyContext();
   context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.id,null,AuthorityUtils.createAuthorityList("ROLE_USER","ROLE_"+account.role)));
   SecurityContextHolder.setContext(context);
  }
  chain.doFilter(req,res);
 }
}
