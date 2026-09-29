package com.meetgrid.controller;

import com.meetgrid.dto.Api.*;
import com.meetgrid.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WorkspaceController {
    private final WorkspaceService workspace;
    public WorkspaceController(WorkspaceService workspace) { this.workspace = workspace; }
    @PostMapping("/members") @ResponseStatus(HttpStatus.CREATED)
    public MemberView addMember(@Valid @RequestBody MemberInput input) { return workspace.addMember(input); }
    @PutMapping("/members/{id}")
    public MemberView updateMember(@PathVariable String id, @Valid @RequestBody MemberInput input) { return workspace.updateMember(id, input); }
    @DeleteMapping("/members/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable String id) { workspace.deleteMember(id); }
    @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED)
    public RoomView addRoom(@Valid @RequestBody RoomInput input) { return workspace.saveRoom(null, input); }
    @PutMapping("/rooms/{id}")
    public RoomView updateRoom(@PathVariable String id, @Valid @RequestBody RoomInput input) { return workspace.saveRoom(id, input); }
    @DeleteMapping("/rooms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable String id) { workspace.deleteRoom(id); }
    @DeleteMapping("/bookings/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@PathVariable String id) { workspace.cancelBooking(id); }
}
