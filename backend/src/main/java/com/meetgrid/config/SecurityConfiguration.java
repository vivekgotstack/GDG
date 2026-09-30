package com.meetgrid.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.*;
@Configuration
public class SecurityConfiguration {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository repository,com.meetgrid.repository.AccountRepository accounts,com.meetgrid.service.RateLimits limits,org.springframework.core.env.Environment env) throws Exception {
   return http.authorizeHttpRequests(a->a
       .requestMatchers("/api/auth/csrf","/api/auth/signup","/api/auth/login","/api/health","/api/plans","/api/site","/api/billing/webhook").permitAll()
       .requestMatchers("/api/auth/forgot-password","/api/auth/reset-password","/api/auth/verify-email","/api/auth/clerk","/actuator/health/**").permitAll()
       .requestMatchers("/api/admin/**").hasRole("ADMIN")
       .requestMatchers("/api/demo/**").denyAll().anyRequest().authenticated())
     .csrf(c->c.ignoringRequestMatchers("/api/billing/webhook"))
     .securityContext(c->c.securityContextRepository(repository))
     .addFilterBefore(new AccountAccessFilter(accounts),org.springframework.security.web.access.intercept.AuthorizationFilter.class)
     .addFilterBefore(new RateLimitFilter(limits,env),AccountAccessFilter.class)
     .requestCache(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable())
     .exceptionHandling(c->c.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Sign in to access your workspace.\"}");})
       .accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Request could not be authorized. Refresh the page and try again.\"}");}))
     .logout(c->c.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,a)->res.setStatus(204)))
     .build();
 }
}
