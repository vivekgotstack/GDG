package com.meetgrid.controller;
import com.meetgrid.config.*;
import com.meetgrid.model.*;
import com.meetgrid.repository.*;
import com.meetgrid.service.PlanService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api")
public class AdminController {
 private final AccountRepository accounts;private final PlanRepository plans;private final SiteContentRepository content;private final AdminEventRepository events;private final MemberRepository members;private final RoomRepository rooms;private final BookingRepository bookings;private final ObjectMapper json;private final AdminBootstrap bootstrap;
 public AdminController(AccountRepository a,PlanRepository p,SiteContentRepository c,AdminEventRepository e,MemberRepository m,RoomRepository r,BookingRepository b,ObjectMapper j,AdminBootstrap boot){accounts=a;plans=p;content=c;events=e;members=m;rooms=r;bookings=b;json=j;bootstrap=boot;}
 public record ContentView(long version,Map<String,String> values){}
 @GetMapping("/site") public ContentView site(){var c=content.findById("public").orElseThrow();try{return new ContentView(c.version,json.readValue(c.payload,new TypeReference<Map<String,String>>(){}));}catch(Exception e){throw new IllegalStateException("Stored site content is unreadable",e);}}
 @PutMapping("/admin/site") @Transactional public ContentView updateSite(@RequestBody ContentView input){
  var c=content.findById("public").orElseThrow();if(c.version!=input.version())throw conflict("Someone updated this content. Reload it before saving.");
  if(input.values()==null||input.values().size()>300)throw bad("Content must contain at most 300 fields.");
  input.values().forEach((key,value)->{
   if(key==null||!key.matches("[a-zA-Z0-9_.-]{1,120}")||value==null||value.length()>80000)throw bad("Invalid content field or value.");
   if(key.equals("brand.companyUrl")){try{var url=java.net.URI.create(value);if(!Set.of("https","http").contains(url.getScheme())||url.getHost()==null||url.getUserInfo()!=null)throw new IllegalArgumentException();}catch(Exception e){throw bad("Company website must be a valid https:// or http:// address.");}}
   if(key.equals("brand.supportEmail")&&!value.matches("[^\\s@]+@[^\\s@]+"))throw bad("Enter a valid support email.");
   if(key.equals("brand.supportPhone")&&!value.matches("[+0-9 ()-]{5,30}"))throw bad("Enter a valid phone number.");
   if(key.startsWith("policy."))validatePolicy(value);
  });
  try{c.payload=json.writeValueAsString(input.values());}catch(Exception e){throw bad("Could not save content.");}
  if(c.payload.length()>400000)throw bad("Site content is too large.");
  content.saveAndFlush(c);audit("Updated site content","public v"+c.version);return site();
 }
 private void validatePolicy(String value){try{
  var p=json.readTree(value);if(!p.path("title").isTextual()||!p.path("description").isTextual()||!p.path("summary").isTextual()||!p.path("sections").isArray()||p.path("sections").size()>30)throw new IllegalArgumentException();
  var ids=new HashSet<String>();for(var s:p.path("sections")){if(!s.path("id").isTextual()||!s.path("id").asText().matches("[a-z0-9-]{1,80}")||!ids.add(s.path("id").asText())||!s.path("title").isTextual()||!s.path("paragraphs").isArray())throw new IllegalArgumentException();for(var text:s.path("paragraphs"))if(!text.isTextual())throw new IllegalArgumentException();if(s.has("points")){if(!s.path("points").isArray())throw new IllegalArgumentException();for(var point:s.path("points"))if(!point.isTextual())throw new IllegalArgumentException();}}
 }catch(Exception e){throw bad("Policy needs a title, description, summary, and sections with unique IDs, titles, and text paragraphs.");}}
 public record PlanInput(@NotBlank @Size(max=80) String name,@NotBlank @Size(max=300) String description,@Min(1) @Max(10000) int monthlyPrice,@Pattern(regexp="USD") String currency,@Min(1) @Max(10000) int members,@Min(1) @Max(1000) int rooms,@Min(1) @Max(100000) int bookings,@Min(1) @Max(1000) int presets,long version){}
 @PutMapping("/admin/plans/{id}") @Transactional public PlanDefinition updatePlan(@PathVariable String id,@Valid @RequestBody PlanInput i){
  var p=plans.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(p.version!=i.version())throw conflict("This plan was changed elsewhere. Reload before saving.");
  p.name=i.name().strip();p.description=i.description().strip();p.monthlyPrice=i.monthlyPrice();p.currency="USD";p.members=i.members();p.rooms=i.rooms();p.bookings=i.bookings();p.presets=i.presets();
  plans.saveAndFlush(p);audit("Updated plan and limits",p.id);return p;
 }
 public record AccountView(String id,String email,String name,String workspaceName,String timezone,String role,boolean suspended,String plan,boolean subscribed) {static AccountView from(Account a){return new AccountView(a.id,a.email,a.displayName,a.workspaceName,a.timezone,a.role,a.suspended,a.plan,a.subscriptionId!=null);}}
 @GetMapping("/admin/users") public Map<String,Object> users(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page){
  var result=accounts.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q.strip(),q.strip(),PageRequest.of(Math.max(0,page),25,Sort.by("email")));
  return Map.of("items",result.getContent().stream().map(AccountView::from).toList(),"total",result.getTotalElements(),"pages",result.getTotalPages(),"page",result.getNumber());
 }
 public record AccountInput(@NotBlank @Size(max=80) String name,@NotBlank @Size(max=100) String workspaceName,@NotBlank @Size(max=80) String timezone,@NotNull @Pattern(regexp="USER|ADMIN") String role,boolean suspended){}
 @PutMapping("/admin/users/{id}") @Transactional public AccountView updateUser(@PathVariable String id,@Valid @RequestBody AccountInput i){
  var a=accounts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if((a.id.equals(WorkspaceIdentity.id())||a.email.equals(bootstrap.email()))&&(!i.role().equals("ADMIN")||i.suspended()))throw conflict("The owner and your own administrator access cannot be disabled here.");
  try{java.time.ZoneId.of(i.timezone());}catch(Exception e){throw bad("Choose a valid IANA timezone.");}
  a.displayName=i.name().strip();a.workspaceName=i.workspaceName().strip();a.timezone=i.timezone();a.role=i.role();a.suspended=i.suspended();accounts.save(a);audit("Updated account: "+a.role+(a.suspended?", suspended":", active"),a.id);return AccountView.from(a);
 }
 @GetMapping("/admin/overview") public Map<String,Long> overview(){return Map.of("accounts",accounts.count(),"members",members.count(),"rooms",rooms.count(),"bookings",bookings.count());}
 @GetMapping("/admin/audit") public List<AdminEvent> audit(){return events.findTop100ByOrderByOccurredAtDesc();}
 private void audit(String action,String target){var event=new AdminEvent();event.id=UUID.randomUUID().toString();event.actorId=WorkspaceIdentity.id();event.action=action;event.target=target;event.occurredAt=java.time.Instant.now();events.save(event);}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
