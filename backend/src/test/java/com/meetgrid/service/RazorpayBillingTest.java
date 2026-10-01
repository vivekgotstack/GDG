package com.meetgrid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetgrid.model.Account;
import com.meetgrid.model.PaymentSubscription;
import com.meetgrid.repository.AccountRepository;
import com.meetgrid.repository.PaymentSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RazorpayBillingTest {
 private final AccountRepository accounts=mock(AccountRepository.class);
 private final PaymentSubscriptionRepository subscriptions=mock(PaymentSubscriptionRepository.class);
 private final PlanService plans=mock(PlanService.class);
 private final RazorpayClient api=mock(RazorpayClient.class);
 private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
 private final ObjectMapper json=new ObjectMapper();
 private final MockEnvironment env=new MockEnvironment();
 private final RazorpayBilling billing=new RazorpayBilling(accounts,subscriptions,plans,api,env,json,jdbc,mock(TransactionTemplate.class));
 private Account account;
 private PaymentSubscription subscription;
 private final Instant end=Instant.now().plusSeconds(86400);

 @BeforeEach void setup(){
  account=new Account();account.id="owner";account.subscriptionId="sub_owned";
  subscription=new PaymentSubscription();subscription.id="sub_owned";subscription.ownerId="owner";subscription.planId="studio";subscription.providerPlanId="plan_studio";subscription.keyId="rzp_test_example";subscription.amount=59900;subscription.currency="INR";
  when(accounts.lockById("owner")).thenReturn(Optional.of(account));
  when(subscriptions.findById("sub_owned")).thenReturn(Optional.of(subscription));
  when(subscriptions.findOwnerById("sub_owned")).thenReturn(Optional.of("owner"));
  when(api.keyId()).thenReturn("rzp_test_example");when(api.keySecret()).thenReturn("billing-test-only-secret");when(api.webhookSecret()).thenReturn("webhook-test-only-secret");
 }
 private com.fasterxml.jackson.databind.JsonNode state(String status,int paid,Instant until){return json.valueToTree(Map.of("id","sub_owned","plan_id","plan_studio","quantity",1,"status",status,"paid_count",paid,"current_end",until.getEpochSecond(),"payment_method","card"));}

 @Test void signatureChecksKnownVectorAndRejectsAlteredRawBody(){
  String expected="5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";
  assertThat(PaymentSignatures.verify("Jefe","what do ya want for nothing?",expected)).isTrue();
  assertThat(PaymentSignatures.verify("Jefe","what do ya want for nothing? ",expected)).isFalse();
  assertThat(PaymentSignatures.verify("different","what do ya want for nothing?",expected)).isFalse();
  assertThat(PaymentSignatures.verify("Jefe","what do ya want for nothing?","bad")).isFalse();
 }
 @Test void forgedPaymentCannotFetchProviderOrGrantAccess(){
  assertThatThrownBy(()->billing.verify("owner","sub_owned","pay_test","0".repeat(64))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  verify(api,never()).get(anyString());assertThat(account.plan).isEqualTo("free");
 }
 @Test void anotherAccountCannotVerifyTheSubscription(){
  var other=new Account();other.id="other";when(accounts.lockById("other")).thenReturn(Optional.of(other));
  assertThatThrownBy(()->billing.verify("other","sub_owned","pay_test","0".repeat(64))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
  verify(api,never()).get(anyString());
 }
 @Test void mandateAuthorizationDoesNotBuyAPlan(){
  billing.sync(account,subscription,state("authenticated",0,end));
  assertThat(account.plan).isEqualTo("free");assertThat(subscription.paidUntil).isNull();
  billing.sync(account,subscription,state("active",0,end));assertThat(account.plan).isEqualTo("free");
 }
 @Test void chargedCycleUnlocksAccessAndRepeatedStateCannotExtendIt(){
  billing.sync(account,subscription,state("active",1,end));
  assertThat(account.plan).isEqualTo("studio");Instant paidUntil=subscription.paidUntil;
  billing.sync(account,subscription,state("active",1,end.plusSeconds(86400)));
  assertThat(subscription.paidUntil).isEqualTo(paidUntil);
 }
 @Test void failedRenewalDoesNotExtendPaidAccess(){
  subscription.paidCount=1;subscription.paidUntil=Instant.now().minusSeconds(10);account.plan="studio";
  billing.sync(account,subscription,state("pending",2,end));
  assertThat(account.plan).isEqualTo("free");assertThat(subscription.paidUntil).isBefore(Instant.now());assertThat(subscription.paidCount).isEqualTo(1);
  billing.sync(account,subscription,state("active",2,end));assertThat(account.plan).isEqualTo("studio");
 }
 @Test void staleAccountPlanCannotBypassPaidPeriodExpiry(){
  account.plan="studio";subscription.paidUntil=Instant.now().minusSeconds(1);
  assertThat(PlanService.effectiveTier(account,subscriptions)).isEqualTo("free");
 }
 @Test void badWebhookCannotReachTheProvider(){
  assertThatThrownBy(()->billing.webhook("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),"0".repeat(64))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  verify(api,never()).get(anyString());
 }
 @Test void alreadyProcessedWebhookDoesNotRepeatProviderCalls() throws Exception {
  byte[] body=json.writeValueAsBytes(Map.of("event","subscription.charged","payload",Map.of("subscription",Map.of("entity",Map.of("id","sub_owned")))));
  billing.webhook(body,sign("webhook-test-only-secret",body));
  verify(api,never()).get(anyString());
 }
 @Test void olderWebhookUsesCurrentProviderState() throws Exception {
  byte[] body=json.writeValueAsBytes(Map.of("event","subscription.activated","payload",Map.of("subscription",Map.of("entity",Map.of("id","sub_owned","status","active")))));
  when(jdbc.update(anyString(),any(Object[].class))).thenReturn(1);
  when(api.get("/subscriptions/sub_owned")).thenReturn(state("cancelled",1,end));
  account.plan="studio";subscription.paidUntil=end;
  billing.webhook(body,sign("webhook-test-only-secret",body));
  assertThat(account.plan).isEqualTo("free");
 }
 @Test void mismatchedPriceCannotCreateSubscription() throws Exception {
  var plan=new PlanService.Plan("studio","Studio","Team",599,"INR",30,10,150,25,true,0);
  when(plans.all()).thenReturn(List.of(plan));when(plans.priceId("studio")).thenReturn("plan_studio");
  when(api.get("/plans/plan_studio")).thenReturn(json.readTree("{\"id\":\"plan_studio\",\"period\":\"monthly\",\"interval\":1,\"item\":{\"amount\":29900,\"currency\":\"INR\"}}"));
  assertThatThrownBy(()->billing.checkout("owner","studio")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  verify(api,never()).post(anyString(),anyMap());
 }
 @Test void cancellationKeepsConfirmedPaidPeriod() {
  subscription.status="active";subscription.paidUntil=end;subscription.paidCount=1;
  when(api.get("/subscriptions/sub_owned")).thenReturn(state("active",1,end));
  when(api.post("/subscriptions/sub_owned/cancel",Map.of("cancel_at_cycle_end",1))).thenReturn(state("active",1,end));
  var status=billing.cancel("owner");assertThat(status.cancelAtPeriodEnd()).isTrue();assertThat(status.paidUntil()).isEqualTo(end);
 }
 @Test void upiSubscriptionCannotScheduleUnsupportedPlanChange() {
  var remote=state("active",1,end).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)remote).put("payment_method","upi");
  when(api.get("/subscriptions/sub_owned")).thenReturn(remote);
  assertThatThrownBy(()->billing.change("owner","starter")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  verify(api,never()).patch(anyString(),anyMap());
 }
 private String sign(String secret,byte[] body) throws Exception {
  var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(body));
 }
}
