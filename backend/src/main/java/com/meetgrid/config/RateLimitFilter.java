package com.meetgrid.config;
import com.meetgrid.service.RateLimits;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
public class RateLimitFilter extends OncePerRequestFilter {
 private final RateLimits limits;private final Environment env;
 public RateLimitFilter(RateLimits l,Environment e){limits=l;env=e;}
 @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().startsWith("/api/")||r.getRequestURI().equals("/api/health")||r.getRequestURI().equals("/api/billing/webhook");}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(req.getContentLengthLong()>1048576){res.sendError(413);return;}
  boolean auth=req.getRequestURI().startsWith("/api/auth/")&&!req.getMethod().equals("GET");
  String identity=client(req);var user=SecurityContextHolder.getContext().getAuthentication();
  if(!auth&&user!=null&&user.isAuthenticated()&&!(user instanceof org.springframework.security.authentication.AnonymousAuthenticationToken))identity="account:"+user.getName();
  int seconds=auth?900:60;
  try{limits.check(auth?"auth":"api",identity,env.getProperty(auth?"meetgrid.limits.auth-per-quarter-hour":"meetgrid.limits.api-per-minute",Integer.class,auth?30:240),seconds);}
  catch(ResponseStatusException e){res.setStatus(e.getStatusCode().value());res.setContentType("application/json");res.setHeader("Retry-After",String.valueOf(seconds));res.getWriter().write("{\"detail\":\""+e.getReason()+"\"}");return;}
  chain.doFilter(req,res);
 }
 private String client(HttpServletRequest req){
  String secret=env.getProperty("meetgrid.proxy-secret","");String ip=req.getHeader("X-MeetGrid-Client"),time=req.getHeader("X-MeetGrid-Time"),sig=req.getHeader("X-MeetGrid-Signature");
  if(secret.length()<32||secret.startsWith("REPLACE_")||ip==null||ip.length()>80||time==null||sig==null)return req.getRemoteAddr();
  try{if(Math.abs(Instant.now().getEpochSecond()-Long.parseLong(time))>60)return req.getRemoteAddr();var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));if(MessageDigest.isEqual(mac.doFinal((ip+"\n"+time).getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(sig)))return ip;}catch(Exception ignored){}
  return req.getRemoteAddr();
 }
}
