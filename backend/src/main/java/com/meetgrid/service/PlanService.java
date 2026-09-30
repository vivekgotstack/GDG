package com.meetgrid.service;
import com.meetgrid.repository.*;
import com.meetgrid.config.WorkspaceIdentity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class PlanService {
 public record Plan(String id,String name,String description,int monthlyPrice,String currency,int members,int rooms,int bookings,int presets,boolean checkoutEnabled,long version){}
 private final Environment env;private final AccountRepository accounts;private final MemberRepository members;private final RoomRepository rooms;private final BookingRepository bookings;private final PlanRepository definitions;private final TemplateRepository templates;
 public PlanService(Environment e,AccountRepository a,MemberRepository m,RoomRepository r,BookingRepository b,PlanRepository d,TemplateRepository t){env=e;accounts=a;members=m;rooms=r;bookings=b;definitions=d;templates=t;}
 public String priceId(String tier){return env.getProperty("STRIPE_PRICE_"+tier.toUpperCase(Locale.ROOT),"");}
 public List<Plan> all(){return List.of("starter","studio","scale").stream().map(id->definitions.findById(id).orElseThrow()).map(p->new Plan(p.id,p.name,p.description,p.monthlyPrice,p.currency,p.members,p.rooms,p.bookings,p.presets,!priceId(p.id).isBlank()&&!env.getProperty("STRIPE_SECRET_KEY","").isBlank()&&!env.getProperty("STRIPE_WEBHOOK_SECRET","").isBlank(),p.version)).toList();}
 public Plan preview(){return new Plan("free","Preview","Explore the workflow before choosing a plan.",0,"USD",5,2,10,3,false,0);}
 public Plan current(){String tier=accounts.findById(WorkspaceIdentity.id()).map(a->a.plan).orElse("free");return all().stream().filter(p->p.id().equals(tier)).findFirst().orElse(preview());}
 public void requireCapacity(String resource){requireCapacity(resource,1);}
 public void requireCapacity(String resource,int adding){
   String id=WorkspaceIdentity.id();if(id.equals("legacy"))return;
   var a=accounts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));if(a.role.equals("ADMIN"))return;
   var plan=current();long used=switch(resource){case "members"->members.countByOwnerId(id);case "rooms"->rooms.countByOwnerId(id);case "presets"->templates.countByOwnerId(id);default->bookings.countByRoomOwnerId(id);};
   int limit=switch(resource){case "members"->plan.members();case "rooms"->plan.rooms();case "presets"->plan.presets();default->plan.bookings();};
   if(used+adding>limit)throw new ResponseStatusException(HttpStatus.CONFLICT,"Your "+plan.name()+" workspace allows "+limit+" "+resource+". Remove unused items or choose another plan.");
 }
}
