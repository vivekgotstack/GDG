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
 public record Plan(String id,String name,int monthlyPrice,String currency,int members,int rooms,int bookings,boolean checkoutEnabled){}
 private final Environment env;private final AccountRepository accounts;private final MemberRepository members;private final RoomRepository rooms;private final BookingRepository bookings;
 public PlanService(Environment e,AccountRepository a,MemberRepository m,RoomRepository r,BookingRepository b){env=e;accounts=a;members=m;rooms=r;bookings=b;}
 public String priceId(String tier){return env.getProperty("STRIPE_PRICE_"+tier.toUpperCase(Locale.ROOT),"");}
 private int value(String key,int fallback){return env.getProperty(key,Integer.class,fallback);}
 public List<Plan> all(){return List.of(plan("free","Free",0,5,2,10),plan("studio","Studio",19,25,10,100),plan("scale","Scale",59,100,50,1000));}
 private Plan plan(String id,String name,int price,int people,int spaces,int reservations){String prefix="PLAN_"+id.toUpperCase(Locale.ROOT)+"_";return new Plan(id,env.getProperty(prefix+"NAME",name),value(prefix+"MONTHLY_PRICE",price),env.getProperty("BILLING_CURRENCY","USD"),value(prefix+"MEMBERS",people),value(prefix+"ROOMS",spaces),value(prefix+"BOOKINGS",reservations),!priceId(id).isBlank()&&!env.getProperty("STRIPE_SECRET_KEY","").isBlank());}
 public Plan current(){String tier=accounts.findById(WorkspaceIdentity.id()).map(a->a.plan).orElse("free");return all().stream().filter(p->p.id().equals(tier)).findFirst().orElse(all().getFirst());}
 public void requireCapacity(String resource){
   String id=WorkspaceIdentity.id();if(id.equals("legacy"))return;
   accounts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
   var plan=current();long used=switch(resource){case "members"->members.countByOwnerId(id);case "rooms"->rooms.countByOwnerId(id);default->bookings.countByRoomOwnerId(id);};
   int limit=switch(resource){case "members"->plan.members();case "rooms"->plan.rooms();default->plan.bookings();};
   if(used>=limit)throw new ResponseStatusException(HttpStatus.CONFLICT,"Your "+plan.name()+" plan allows "+limit+" "+resource+". Remove an unused item or choose another plan.");
 }
}
