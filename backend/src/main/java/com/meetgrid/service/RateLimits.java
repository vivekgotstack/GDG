package com.meetgrid.service;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
@Service
public class RateLimits {
 private final StringRedisTemplate redis;
 private static final DefaultRedisScript<Long> WINDOW=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n",Long.class);
 public RateLimits(StringRedisTemplate redis){this.redis=redis;}
 public void check(String scope,String identity,int limit,int seconds){
  Long count;
  try{count=redis.execute(WINDOW,List.of("meetgrid:limit:"+scope+":"+hash(identity)),String.valueOf(seconds));}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Account protection is temporarily unavailable. Try again shortly.");}
  if(count==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Account protection is temporarily unavailable.");
  if(count>limit)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many requests. Please wait before trying again.");
 }
 public static String hash(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
