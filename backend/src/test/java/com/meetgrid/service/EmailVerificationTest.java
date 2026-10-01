package com.meetgrid.service;
import com.meetgrid.model.Account;
import com.meetgrid.repository.AccountRepository;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class EmailVerificationTest {
 private final AccountRepository accounts=mock(AccountRepository.class);
 private final JdbcTemplate db=mock(JdbcTemplate.class);
 private final MailOutbox mail=mock(MailOutbox.class);
 private final AccountSecurity security=new AccountSecurity(accounts,db,mail,mock(RateLimits.class),mock(PasswordEncoder.class),new MockEnvironment());
 private final Account account=new Account();
 @BeforeEach void setup(){account.id="owner";account.email="owner@example.test";when(accounts.lockById("owner")).thenReturn(Optional.of(account));}
 private void link(String purpose){when(db.queryForList(anyString(),eq(RateLimits.hash("test-token")),eq(purpose))).thenReturn(List.of(Map.of("account_id","owner")));}
 @Test void firstConfirmationPersistsAndRepeatedClickRemainsSuccessful(){
  link("VERIFY");when(db.update(startsWith("UPDATE auth_tokens SET consumed=TRUE WHERE token_hash="),any(),any(),any())).thenReturn(1);
  security.consume("test-token","VERIFY",null);assertThat(account.emailVerified).isTrue();verify(accounts).save(account);
  when(db.update(startsWith("UPDATE auth_tokens SET consumed=TRUE WHERE token_hash="),any(),any(),any())).thenReturn(0);
  assertThatCode(()->security.consume("test-token","VERIFY",null)).doesNotThrowAnyException();verify(accounts,times(1)).save(account);
 }
 @Test void resendKeepsEarlierVerificationLinksButReplacesResetLinks(){
  security.issue(account,"VERIFY");verify(db,never()).update(eq("UPDATE auth_tokens SET consumed=TRUE WHERE account_id=? AND purpose=?"),any(),any());
  security.issue(account,"RESET");verify(db).update("UPDATE auth_tokens SET consumed=TRUE WHERE account_id=? AND purpose=?","owner","RESET");
 }
 @Test void expiredOrWrongPurposeTokenCannotVerifyEvenAnAlreadyVerifiedAccount(){
  account.emailVerified=true;assertThatThrownBy(()->security.consume("unknown","VERIFY",null)).isInstanceOf(ResponseStatusException.class);verify(accounts,never()).save(any());
  verify(db).queryForList("SELECT account_id FROM auth_tokens WHERE token_hash=? AND purpose=? AND expires_at>CURRENT_TIMESTAMP",RateLimits.hash("unknown"),"VERIFY");
 }
 @Test void usedUnverifiedLinkRequiresFreshLinkAndNeverGrantsAccess(){
  link("VERIFY");assertThatThrownBy(()->security.consume("test-token","VERIFY",null)).isInstanceOf(ResponseStatusException.class);assertThat(account.emailVerified).isFalse();verify(accounts,never()).save(any());
 }
 @Test void resetReplayAndSuspendedAccountsStayBlocked(){
  link("RESET");account.emailVerified=true;assertThatThrownBy(()->security.consume("test-token","RESET","new-long-password")).isInstanceOf(ResponseStatusException.class);
  link("VERIFY");account.suspended=true;assertThatThrownBy(()->security.consume("test-token","VERIFY",null)).isInstanceOf(ResponseStatusException.class);verify(accounts,never()).save(any());
 }
}
