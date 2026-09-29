package com.meetgrid.controller;
import com.meetgrid.service.PlanService;
import com.meetgrid.repository.*;
import com.meetgrid.config.WorkspaceIdentity;
import com.fasterxml.jackson.databind.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController @RequestMapping("/api")
public class BillingController {
 private final PlanService plans;private final Environment env;private final AccountRepository accounts;private final ObjectMapper json;
 public BillingController(PlanService p,Environment e,AccountRepository a,ObjectMapper j){plans=p;env=e;accounts=a;json=j;}
 @GetMapping("/plans") public List<PlanService.Plan> plans(){return plans.all();}
 public record Checkout(String plan){}
 @PostMapping("/billing/checkout") public Map<String,String> checkout(@RequestBody Checkout input){
   var plan=plans.all().stream().filter(p->p.id().equals(input.plan())&&!p.id().equals("free")).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a paid plan."));
   if(!plan.checkoutEnabled())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Paid checkout is not configured yet. Your free workspace remains available.");
   var account=accounts.findById(WorkspaceIdentity.id()).orElseThrow();
   if(account.subscriptionId!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Manage your existing subscription through billing settings.");
   var form=new LinkedMultiValueMap<String,String>();form.add("mode","subscription");form.add("line_items[0][price]",plans.priceId(plan.id()));form.add("line_items[0][quantity]","1");
   form.add("client_reference_id",account.id);form.add("subscription_data[metadata][account_id]",account.id);
   if(account.stripeCustomer!=null)form.add("customer",account.stripeCustomer);else form.add("customer_email",account.email);
   String origin=env.getProperty("APP_URL","http://localhost:3000");form.add("success_url",origin+"/app/billing?checkout=success");form.add("cancel_url",origin+"/pricing");
   return Map.of("url",stripe("checkout/sessions",form).path("url").asText());
 }
 @PostMapping("/billing/portal") public Map<String,String> portal(){
   var a=accounts.findById(WorkspaceIdentity.id()).orElseThrow();if(a.stripeCustomer==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"No billing account yet.");
   var form=new LinkedMultiValueMap<String,String>();form.add("customer",a.stripeCustomer);form.add("return_url",env.getProperty("APP_URL","http://localhost:3000")+"/app/billing");return Map.of("url",stripe("billing_portal/sessions",form).path("url").asText());
 }
 private JsonNode stripe(String path,LinkedMultiValueMap<String,String> form){
   String key=env.getProperty("STRIPE_SECRET_KEY","");if(key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Billing is not configured.");
   try{return RestClient.create("https://api.stripe.com/v1").post().uri("/"+path).header("Authorization","Bearer "+key).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider is unavailable or billing configuration needs attention.");}
 }
 @PostMapping("/billing/webhook") @Transactional public Map<String,Boolean> webhook(@RequestBody String body,@RequestHeader(value="Stripe-Signature",defaultValue="") String signature){
   String secret=env.getProperty("STRIPE_WEBHOOK_SECRET","");if(secret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
   JsonNode event;
   try {
     var parts=Arrays.stream(signature.split(",")).map(s->s.split("=",2)).filter(p->p.length==2).toList();
     String ts=parts.stream().filter(p->p[0].equals("t")).findFirst().orElseThrow()[1];
     if(Math.abs(java.time.Instant.now().getEpochSecond()-Long.parseLong(ts))>300)throw new IllegalArgumentException();
     var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
     byte[] expected=mac.doFinal((ts+"."+body).getBytes(StandardCharsets.UTF_8));
     boolean valid=parts.stream().filter(p->p[0].equals("v1")).anyMatch(p->{try{return java.security.MessageDigest.isEqual(expected,HexFormat.of().parseHex(p[1]));}catch(Exception e){return false;}});
     if(!valid)throw new IllegalArgumentException();event=json.readTree(body);
   }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid webhook signature.");}
   String type=event.path("type").asText();
   if(type.equals("customer.subscription.created")||type.equals("customer.subscription.updated")||type.equals("customer.subscription.deleted")){
     var subscription=event.path("data").path("object");String accountId=subscription.path("metadata").path("account_id").asText();
     var account=accounts.lockById(accountId).orElse(null);
     if(account!=null && event.path("created").asLong()>=account.billingEventTime){
       String price=subscription.path("items").path("data").path(0).path("price").path("id").asText();
       String tier=plans.all().stream().filter(p->!p.id().equals("free")&&!plans.priceId(p.id()).isBlank()&&plans.priceId(p.id()).equals(price)).map(PlanService.Plan::id).findFirst().orElse("free");
       String status=subscription.path("status").asText();boolean active=!type.endsWith("deleted")&&(status.equals("active")||status.equals("trialing"));
       String subId=subscription.path("id").asText();
       if(account.subscriptionId==null||account.subscriptionId.equals(subId)){
         account.plan=active?tier:"free";account.stripeCustomer=subscription.path("customer").asText();account.subscriptionId=type.endsWith("deleted")?null:subId;account.billingEventTime=event.path("created").asLong();accounts.save(account);
       }
     }
   }
   return Map.of("received",true);
 }
}
