package com.meetgrid.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetgrid.model.Account;
import com.meetgrid.model.PaymentSubscription;
import com.meetgrid.repository.AccountRepository;
import com.meetgrid.repository.PaymentSubscriptionRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class RazorpayBilling {
 private final AccountRepository accounts;
 private final PaymentSubscriptionRepository subscriptions;
 private final PlanService plans;
 private final RazorpayClient api;
 private final Environment env;
 private final ObjectMapper json;
 private final JdbcTemplate jdbc;
 private final TransactionTemplate transactions;
 private static final Set<String> EVENTS=Set.of("subscription.authenticated","subscription.activated","subscription.charged","subscription.pending","subscription.halted","subscription.paused","subscription.resumed","subscription.cancelled","subscription.completed","subscription.updated");
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(RazorpayBilling.class);
 public RazorpayBilling(AccountRepository a,PaymentSubscriptionRepository s,PlanService p,RazorpayClient r,Environment e,ObjectMapper j,JdbcTemplate db,TransactionTemplate tx){accounts=a;subscriptions=s;plans=p;api=r;env=e;json=j;jdbc=db;transactions=tx;}
 public record Checkout(String keyId,String subscriptionId,String planName,int amount,String currency,int cycles){}
 public record Status(String subscriptionId,String planId,String status,Instant paidUntil,Instant currentEnd,boolean cancelAtPeriodEnd,String pendingPlanId,String paymentMethod,boolean canChangePlan,int amount,String currency,String lastPaymentId){}
 private Account owner(String id){return accounts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again."));}
 private PlanService.Plan plan(String id){return plans.all().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->bad("Choose a paid plan."));}
 private PaymentSubscription current(Account a){return a.subscriptionId==null?null:subscriptions.findById(a.subscriptionId).filter(s->s.ownerId.equals(a.id)).orElse(null);}
 private int cycles(){int value=env.getProperty("meetgrid.billing.subscription-cycles",Integer.class,120);if(value<1||value>120)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Billing cycle configuration must be between 1 and 120 months.");return value;}

 @Transactional public Checkout checkout(String ownerId,String tier){
  api.requireConfigured();
  var a=owner(ownerId);var selected=plan(tier);validatePlan(selected);
  var existing=current(a);
  if(existing!=null){
   sync(a,existing,api.get("/subscriptions/"+existing.id));
   if(!existing.terminal()){
    if(existing.status.equals("created")&&existing.planId.equals(tier)&&existing.providerPlanId.equals(plans.priceId(tier))&&existing.amount==selected.monthlyPrice()*100&&existing.currency.equals(selected.currency())&&existing.keyId.equals(api.keyId()))return checkoutView(existing,selected);
    throw conflict("You already have a subscription or unfinished checkout. Manage it in Plans & billing before starting another.");
   }
   if(existing.hasAccess(Instant.now()))throw conflict("Your current paid period has not ended. Start the next subscription after it expires.");
  }
  var remote=api.post("/subscriptions",Map.of("plan_id",plans.priceId(tier),"quantity",1,"total_count",cycles(),"customer_notify",true,"expire_by",Instant.now().plusSeconds(86400).getEpochSecond(),"notes",Map.of("account_id",a.id,"meetgrid_plan",tier)));
  String id=remote.path("id").asText();if(!id.matches("sub_[a-zA-Z0-9]+")||!plans.priceId(tier).equals(remote.path("plan_id").asText())||!remote.path("status").asText().equals("created"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay returned an unexpected subscription.");
  var local=new PaymentSubscription();local.id=id;local.ownerId=a.id;local.planId=tier;local.providerPlanId=plans.priceId(tier);local.keyId=api.keyId();local.amount=selected.monthlyPrice()*100;local.currency=selected.currency();
  subscriptions.saveAndFlush(local);a.subscriptionId=id;accounts.save(a);
  return checkoutView(local,selected);
 }
 private Checkout checkoutView(PaymentSubscription s,PlanService.Plan p){return new Checkout(s.keyId,s.id,p.name(),s.amount,s.currency,cycles());}

 void validatePlan(PlanService.Plan selected){
  String providerId=plans.priceId(selected.id());
  if(!selected.checkoutEnabled()||!providerId.matches("plan_[a-zA-Z0-9]+"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"This plan's Razorpay checkout is not configured.");
  JsonNode remote=api.get("/plans/"+providerId);
  if(!providerId.equals(remote.path("id").asText())||!remote.path("period").asText().equals("monthly")||remote.path("interval").asInt()!=1||remote.path("item").path("amount").asInt(-1)!=selected.monthlyPrice()*100||!remote.path("item").path("currency").asText().equalsIgnoreCase(selected.currency()))throw conflict("The Razorpay plan must match the displayed monthly price and currency. Contact billing support.");
 }

 @Transactional public Status verify(String ownerId,String subscriptionId,String paymentId,String signature){
  api.requireConfigured();var a=owner(ownerId);var local=ownedCurrent(a,subscriptionId);
  // The saved subscription ID is authoritative; never build the HMAC from an arbitrary client ID.
  if(!paymentId.matches("pay_[a-zA-Z0-9]+")||!PaymentSignatures.verify(api.keySecret(),paymentId+"|"+local.id,signature))throw bad("Payment signature could not be verified.");
  var payment=api.get("/payments/"+paymentId);
  if(!paymentId.equals(payment.path("id").asText())||!Set.of("authorized","captured").contains(payment.path("status").asText()))throw bad("Payment has not been authorized or captured.");
  local.lastPaymentId=paymentId;
  if(payment.hasNonNull("method"))local.paymentMethod=payment.path("method").asText();
  sync(a,local,api.get("/subscriptions/"+local.id));
  return view(local);
 }

 @Transactional public Status status(String ownerId,boolean refresh){
  var a=owner(ownerId);var local=current(a);if(local==null)return null;
  if(refresh)sync(a,local,api.get("/subscriptions/"+local.id));
  return view(local);
 }
 @Transactional public Status cancel(String ownerId){
  api.requireConfigured();var a=owner(ownerId);var local=current(a);if(local==null)throw conflict("There is no Razorpay subscription to cancel.");
  sync(a,local,api.get("/subscriptions/"+local.id));
  if(local.terminal()||local.cancelAtPeriodEnd)return view(local);
  if(local.pendingPlanId!=null)cancelUpdateLocked(a,local);
  boolean atEnd=local.status.equals("active");
  var remote=api.post("/subscriptions/"+local.id+"/cancel",Map.of("cancel_at_cycle_end",atEnd?1:0));
  local.cancelAtPeriodEnd=atEnd;
  sync(a,local,remote);
  if(!atEnd){local.paidUntil=Instant.now();a.plan="free";subscriptions.save(local);accounts.save(a);}
  return view(local);
 }
 @Transactional public Status cancelUpdate(String ownerId){
  api.requireConfigured();var a=owner(ownerId);var local=current(a);if(local==null)throw conflict("There is no subscription to update.");
  sync(a,local,api.get("/subscriptions/"+local.id));
  if(local.pendingPlanId==null)throw conflict("There is no pending plan change.");
  cancelUpdateLocked(a,local);return view(local);
 }
 private void cancelUpdateLocked(Account a,PaymentSubscription local){
  var remote=api.post("/subscriptions/"+local.id+"/cancel_scheduled_changes",Map.of());
  if(remote.path("has_scheduled_changes").asBoolean())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay could not confirm cancellation of the plan change.");
  local.pendingPlanId=null;local.pendingProviderPlanId=null;local.pendingAmount=null;local.pendingCurrency=null;
  sync(a,local,remote);
 }
 @Transactional public Status change(String ownerId,String tier){
  api.requireConfigured();var a=owner(ownerId);var local=current(a);if(local==null)throw conflict("Start a subscription before changing it.");
  sync(a,local,api.get("/subscriptions/"+local.id));
  if(!canChange(local))throw conflict("This subscription cannot change plans in place. Cancel renewal and choose a new plan after the paid period, or contact billing support.");
  var selected=plan(tier);if(local.planId.equals(tier))throw conflict("You are already on this plan.");validatePlan(selected);
  local.pendingPlanId=tier;local.pendingProviderPlanId=plans.priceId(tier);local.pendingAmount=selected.monthlyPrice()*100;local.pendingCurrency=selected.currency();
  var remote=api.patch("/subscriptions/"+local.id,Map.of("plan_id",local.pendingProviderPlanId,"quantity",1,"schedule_change_at","cycle_end","customer_notify",true));
  if(!remote.path("has_scheduled_changes").asBoolean())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay did not confirm the scheduled plan change. Contact billing support before retrying.");
  sync(a,local,remote);return view(local);
 }
 private PaymentSubscription ownedCurrent(Account a,String id){var local=current(a);if(local==null||!local.id.equals(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Checkout does not belong to this workspace.");return local;}
 private boolean canChange(PaymentSubscription s){return s.status.equals("active")&&!s.cancelAtPeriodEnd&&s.pendingPlanId==null&&"card".equals(s.paymentMethod);}
 private Status view(PaymentSubscription s){return new Status(s.id,s.planId,s.status,s.paidUntil,s.currentEnd,s.cancelAtPeriodEnd,s.pendingPlanId,s.paymentMethod,canChange(s),s.amount,s.currency,s.lastPaymentId);}

 // Every notification triggers a fresh provider read. Delayed or reordered events cannot restore an old state.
 @Transactional public void webhook(byte[] body,String signature){
  if(!RazorpayClient.configured(api.webhookSecret()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Webhook is not configured.");
  if(!PaymentSignatures.verify(api.webhookSecret(),body,signature))throw bad("Invalid webhook signature.");
  JsonNode event;String eventId;
  try{event=json.readTree(body);eventId=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));}catch(Exception e){throw bad("Invalid webhook body.");}
  if(event==null||!event.isObject())throw bad("Invalid webhook body.");
  if(!EVENTS.contains(event.path("event").asText()))return;
  var entity=event.path("payload").path("subscription").path("entity");String id=entity.path("id").asText();
  if(!id.matches("sub_[a-zA-Z0-9]+"))throw bad("Missing subscription identifier.");
  var ownerId=subscriptions.findOwnerById(id).orElse(null);
  if(ownerId==null)return; // Ignore subscriptions created outside this application.
  var a=owner(ownerId);var local=subscriptions.findById(id).orElseThrow();
  if(jdbc.update("INSERT INTO payment_webhook_events(id,processed_at) VALUES (?,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",eventId)==0)return;
  sync(a,local,api.get("/subscriptions/"+local.id));
 }

 void sync(Account a,PaymentSubscription local,JsonNode remote){
  if(!local.id.equals(remote.path("id").asText())||remote.path("quantity").asInt()!=1)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unexpected subscription details. Contact billing support.");
  String providerPlan=remote.path("plan_id").asText();
  if(!local.providerPlanId.equals(providerPlan)){
   if(local.pendingProviderPlanId==null||!local.pendingProviderPlanId.equals(providerPlan))throw conflict("This subscription's provider plan changed outside MeetGrid. Contact billing support.");
   local.planId=local.pendingPlanId;local.providerPlanId=local.pendingProviderPlanId;local.amount=local.pendingAmount;local.currency=local.pendingCurrency;
   local.pendingPlanId=null;local.pendingProviderPlanId=null;local.pendingAmount=null;local.pendingCurrency=null;
  }
  String state=remote.path("status").asText();if(!Set.of("created","authenticated","active","pending","halted","cancelled","completed","expired","paused").contains(state))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unknown subscription status.");
  int paid=remote.path("paid_count").asInt(0);long end=remote.path("current_end").asLong(0);
  // Authorizing a mandate does not buy a plan. Never extend access for an unpaid renewal.
  if(state.equals("active")&&paid>local.paidCount&&end>Instant.now().getEpochSecond()){local.paidUntil=Instant.ofEpochSecond(end);local.paidCount=paid;}
  local.status=state;local.currentEnd=end>0?Instant.ofEpochSecond(end):null;
  if(remote.hasNonNull("payment_method"))local.paymentMethod=remote.path("payment_method").asText();
  if(state.equals("cancelled")&&!local.cancelAtPeriodEnd)local.paidUntil=Instant.now();
  local.syncedAt=Instant.now();subscriptions.save(local);
  if(local.id.equals(a.subscriptionId)){a.plan=local.hasAccess(Instant.now())?local.planId:"free";accounts.save(a);}
 }

 @Scheduled(initialDelay=60000,fixedDelay=300000) public void reconcile(){
  if(!api.configured())return;
  var due=subscriptions.findTop20ByStatusInAndSyncedAtBeforeOrderBySyncedAtAsc(List.of("created","authenticated","active","pending","halted","paused"),Instant.now().minusSeconds(300));
  for(var candidate:due){try{
   transactions.executeWithoutResult(tx->{var a=owner(candidate.ownerId);var local=subscriptions.findById(candidate.id).orElseThrow();sync(a,local,api.get("/subscriptions/"+candidate.id));});
  }catch(Exception e){log.warn("Razorpay reconciliation deferred for {}",candidate.id);break;}}
 }
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
 private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
}
