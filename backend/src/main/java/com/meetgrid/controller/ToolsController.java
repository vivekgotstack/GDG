package com.meetgrid.controller;
import com.meetgrid.config.WorkspaceIdentity;
import com.meetgrid.dto.Api.*;
import com.meetgrid.model.Member;
import com.meetgrid.repository.*;
import com.meetgrid.service.PlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/tools")
public class ToolsController {
 private final MemberRepository members;private final RoomRepository rooms;private final BookingRepository bookings;private final TemplateRepository templates;private final AccountRepository accounts;private final PlanService plans;
 public ToolsController(MemberRepository m,RoomRepository r,BookingRepository b,TemplateRepository t,AccountRepository a,PlanService p){members=m;rooms=r;bookings=b;templates=t;accounts=a;plans=p;}
 public record Import(@NotEmpty @Size(max=100) List<@NotBlank @Size(max=80) String> names){}
 @PostMapping("/import-members") @Transactional @ResponseStatus(HttpStatus.CREATED)
 public List<MemberView> importMembers(@Valid @RequestBody Import input){
  String owner=WorkspaceIdentity.id();accounts.lockById(owner).orElseThrow();
  var existing=new HashSet<String>();members.findByOwnerId(owner).forEach(m->existing.add(m.name.strip().toLowerCase(Locale.ROOT)));
  var names=input.names().stream().map(String::strip).toList();
  for(String name:names)if(!existing.add(name.toLowerCase(Locale.ROOT)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Duplicate or existing name: "+name+". Refresh the preview and try again.");
  plans.requireCapacity("members",names.size());String[] colors={"lavender","pink","apricot","blue","sage"};var created=new ArrayList<Member>();
  for(int i=0;i<names.size();i++){var m=new Member(UUID.randomUUID().toString(),names.get(i),colors[i%colors.length],List.of());m.ownerId=owner;created.add(m);}
  return members.saveAll(created).stream().map(MemberView::from).toList();
 }
 @GetMapping("/export") @Transactional(readOnly=true) public Map<String,Object> export(){
  String id=WorkspaceIdentity.id();return Map.of("schemaVersion",1,"exportedAt",java.time.Instant.now().toString(),"workspace",AuthController.UserView.from(accounts.findById(id).orElseThrow()),"members",members.findByOwnerId(id).stream().map(MemberView::from).toList(),"rooms",rooms.findByOwnerId(id).stream().map(RoomView::from).toList(),"bookings",bookings.findByRoomOwnerId(id).stream().map(BookingView::from).toList(),"presets",templates.findByOwnerId(id));
 }
 @GetMapping("/usage") public Map<String,Object> usage(){String id=WorkspaceIdentity.id();return Map.of("plan",plans.current(),"members",members.countByOwnerId(id),"rooms",rooms.countByOwnerId(id),"bookings",bookings.countByRoomOwnerId(id),"presets",templates.countByOwnerId(id));}
}
