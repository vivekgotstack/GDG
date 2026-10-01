package com.meetgrid.controller;

import com.meetgrid.model.Account;
import com.meetgrid.repository.AccountRepository;
import com.meetgrid.config.WorkspaceIdentity;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;
  private final com.meetgrid.config.SessionLogin sessions;
  private final com.meetgrid.service.AccountSecurity security;
  private final com.meetgrid.service.RateLimits limits;
  private final com.meetgrid.config.AdminBootstrap bootstrap;
  private final com.meetgrid.repository.PaymentSubscriptionRepository subscriptions;

  public AuthController(AccountRepository a, PasswordEncoder p, com.meetgrid.config.SessionLogin c,
      com.meetgrid.config.AdminBootstrap b, com.meetgrid.service.AccountSecurity s, com.meetgrid.service.RateLimits l,
      com.meetgrid.repository.PaymentSubscriptionRepository sub) {
    accounts = a;
    encoder = p;
    sessions = c;
    bootstrap = b;
    security = s;
    limits = l;
    subscriptions = sub;
  }

  public record Signup(@NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password, @NotBlank @Size(max = 80) String name,
      @NotBlank @Size(max = 100) String workspaceName, @NotBlank @Size(max = 80) String timezone) {
  }

  public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {
  }

  public record Profile(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 100) String workspaceName,
      @NotBlank @Size(max = 80) String timezone) {
  }

  public record UserView(String id, String email, String name, String workspaceName, String timezone, String plan,
      String role, boolean emailVerified, boolean hasPassword, boolean socialLinked) {
    static UserView from(Account a) {
      return new UserView(a.id, a.email, a.displayName, a.workspaceName, a.timezone, a.plan, a.role, a.emailVerified,
          a.hasPassword, a.clerkSubject != null);
    }
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  @GetMapping("/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/auth/signup")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public UserView signup(@Valid @RequestBody Signup input, HttpServletRequest req, HttpServletResponse res) {
    validateTimezone(input.timezone());
    security.ready();
    com.meetgrid.service.AccountSecurity.validatePassword(input.password());
    if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes.");
    String email = input.email().strip().toLowerCase(Locale.ROOT);
    if (email.equals(bootstrap.email()))
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "This identifier is reserved. Sign in with the administrator credentials.");
    if (accounts.findByEmail(email).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "An account with this email already exists. Sign in instead.");
    var a = new Account();
    a.id = UUID.randomUUID().toString();
    a.email = email;
    a.passwordHash = encoder.encode(input.password());
    a.displayName = input.name().strip();
    a.workspaceName = input.workspaceName().strip();
    a.timezone = input.timezone();
    try {
      accounts.saveAndFlush(a);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
    }
    security.issue(a, "VERIFY");
    sessions.authenticate(a, req, res);
    return UserView.from(a);
  }

  @PostMapping("/auth/login")
  public UserView login(@Valid @RequestBody Login input, HttpServletRequest req, HttpServletResponse res) {
    limits.check("login-email", input.email().strip().toLowerCase(Locale.ROOT), 15, 900);
    var a = accounts.findByEmail(input.email().strip().toLowerCase(Locale.ROOT)).orElse(null);
    if (a == null || a.suspended || !a.hasPassword || !encoder.matches(input.password(), a.passwordHash))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Email or password is incorrect, or the account is unavailable.");
    sessions.authenticate(a, req, res);
    return UserView.from(a);
  }

  @GetMapping("/auth/me")
  public UserView me() {
    var a = current();
    var v = UserView.from(a);
    return new UserView(v.id(), v.email(), v.name(), v.workspaceName(), v.timezone(),
        com.meetgrid.service.PlanService.effectiveTier(a, subscriptions), v.role(), v.emailVerified(), v.hasPassword(),
        v.socialLinked());
  }

  @PutMapping("/workspace")
  @Transactional
  public UserView profile(@Valid @RequestBody Profile input) {
    validateTimezone(input.timezone());
    var a = current();
    a.displayName = input.name().strip();
    a.workspaceName = input.workspaceName().strip();
    a.timezone = input.timezone();
    return UserView.from(accounts.save(a));
  }

  private Account current() {
    return accounts.findById(WorkspaceIdentity.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again."));
  }

  private void validateTimezone(String tz) {
    try {
      java.time.ZoneId.of(tz);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid IANA timezone, such as Asia/Kolkata.");
    }
  }
}
