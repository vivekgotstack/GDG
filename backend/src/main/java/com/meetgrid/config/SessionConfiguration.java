package com.meetgrid.config;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.session.web.http.DefaultCookieSerializer;
@Configuration
public class SessionConfiguration {
 @Bean DefaultCookieSerializer cookieSerializer(Environment env){
  var cookie=new DefaultCookieSerializer();cookie.setCookieName("JSESSIONID");cookie.setCookiePath("/");
  cookie.setUseHttpOnlyCookie(true);cookie.setSameSite("Lax");cookie.setUseSecureCookie(env.getProperty("SESSION_COOKIE_SECURE",Boolean.class,true));return cookie;
 }
}
