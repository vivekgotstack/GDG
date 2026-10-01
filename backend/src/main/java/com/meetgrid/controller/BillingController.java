package com.meetgrid.controller;

import com.meetgrid.config.WorkspaceIdentity;
import com.meetgrid.service.PlanService;
import com.meetgrid.service.RazorpayBilling;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api")
public class BillingController {
 private final PlanService plans;private final RazorpayBilling billing;
 public BillingController(PlanService p,RazorpayBilling b){plans=p;billing=b;}
 @GetMapping("/plans") public List<PlanService.Plan> plans(){return plans.all();}
 public record Checkout(@NotBlank @Pattern(regexp="starter|studio|scale") String plan){}
 public record Verification(@NotBlank @Pattern(regexp="sub_[a-zA-Z0-9]{1,80}") String razorpay_subscription_id,@NotBlank @Pattern(regexp="pay_[a-zA-Z0-9]{1,80}") String razorpay_payment_id,@NotBlank @Pattern(regexp="[a-fA-F0-9]{64}") String razorpay_signature){}
 @PostMapping("/billing/checkout") public RazorpayBilling.Checkout checkout(@Valid @RequestBody Checkout input){return billing.checkout(WorkspaceIdentity.id(),input.plan());}
 @PostMapping("/billing/verify") public RazorpayBilling.Status verify(@Valid @RequestBody Verification input){return billing.verify(WorkspaceIdentity.id(),input.razorpay_subscription_id(),input.razorpay_payment_id(),input.razorpay_signature());}
 @GetMapping("/billing/status") public Map<String,Object> status(){var status=billing.status(WorkspaceIdentity.id(),false);return status==null?Map.of():Map.of("subscription",status);}
 @PostMapping("/billing/refresh") public Map<String,Object> refresh(){var status=billing.status(WorkspaceIdentity.id(),true);return status==null?Map.of():Map.of("subscription",status);}
 @PostMapping("/billing/cancel") public RazorpayBilling.Status cancel(){return billing.cancel(WorkspaceIdentity.id());}
 @PostMapping("/billing/cancel-change") public RazorpayBilling.Status cancelChange(){return billing.cancelUpdate(WorkspaceIdentity.id());}
 @PostMapping("/billing/change") public RazorpayBilling.Status change(@Valid @RequestBody Checkout input){return billing.change(WorkspaceIdentity.id(),input.plan());}
 @PostMapping("/billing/webhook") public Map<String,Boolean> webhook(@RequestBody byte[] body,@RequestHeader(value="X-Razorpay-Signature",defaultValue="") String signature){billing.webhook(body,signature);return Map.of("received",true);}
}
