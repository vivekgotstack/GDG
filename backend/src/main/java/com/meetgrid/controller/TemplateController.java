package com.meetgrid.controller;
import com.meetgrid.model.MeetingTemplate;
import com.meetgrid.repository.TemplateRepository;
import com.meetgrid.config.WorkspaceIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/templates")
public class TemplateController {
 private final TemplateRepository templates;
 public TemplateController(TemplateRepository t){templates=t;}
 public record Input(@NotBlank @Size(max=100) String name,@NotNull @Size(max=500) String description,@Min(15) @Max(480) int durationMinutes,@Min(1) @Max(1000) int capacity){}
 @GetMapping public List<MeetingTemplate> list(){return templates.findByOwnerId(WorkspaceIdentity.id());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public MeetingTemplate add(@Valid @RequestBody Input i){var t=new MeetingTemplate();t.id=UUID.randomUUID().toString();t.ownerId=WorkspaceIdentity.id();return save(t,i);}
 @PutMapping("/{id}") public MeetingTemplate update(@PathVariable String id,@Valid @RequestBody Input i){return save(find(id),i);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){templates.delete(find(id));}
 private MeetingTemplate find(String id){return templates.findByIdAndOwnerId(id,WorkspaceIdentity.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Preset not found."));}
 private MeetingTemplate save(MeetingTemplate t,Input i){t.name=i.name().strip();t.description=i.description().strip();t.durationMinutes=i.durationMinutes();t.capacity=i.capacity();return templates.save(t);}
}
