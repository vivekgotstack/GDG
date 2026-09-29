package com.meetgrid.controller;

import com.meetgrid.dto.Api.*;
import com.meetgrid.repository.RoomRepository;
import com.meetgrid.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MeetGridController {
    private final AvailabilityService availability;
    private final MeetingSearchService search;
    private final BookingService bookings;
    private final RoomRepository rooms;
    public MeetGridController(AvailabilityService availability, MeetingSearchService search, BookingService bookings, RoomRepository rooms) {
        this.availability = availability; this.search = search; this.bookings = bookings; this.rooms = rooms;
    }
    @GetMapping("/members")
    public List<MemberView> members() { return availability.list(); }
    @GetMapping("/rooms")
    public List<RoomView> rooms() {
        return rooms.findByOwnerId(com.meetgrid.config.WorkspaceIdentity.id()).stream().sorted(Comparator.comparing(r -> r.name)).map(RoomView::from).toList();
    }
    @PutMapping("/members/{id}/availability")
    public MemberView update(@PathVariable String id, @Valid @RequestBody AvailabilityUpdate request) { return availability.update(id, request); }
    @PostMapping("/meeting-options/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) { return search.search(request); }
    @GetMapping("/bookings")
    public List<BookingView> bookings() { return bookings.list(); }
    @PostMapping("/bookings") @ResponseStatus(HttpStatus.CREATED)
    public BookingView add(@Valid @RequestBody BookingInput request) { return bookings.add(request); }
}
