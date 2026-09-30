package com.meetgrid.service;

import com.meetgrid.dto.Api.*;
import com.meetgrid.model.RoomBooking;
import com.meetgrid.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.meetgrid.service.Intervals.*;

@Service
public class BookingService {
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final PlanService plans;
    public BookingService(RoomRepository rooms, BookingRepository bookings, PlanService plans) { this.rooms = rooms; this.bookings = bookings; this.plans = plans; }
    @Transactional(readOnly = true)
    public List<BookingView> list() {
        return bookings.findByRoomOwnerId(com.meetgrid.config.WorkspaceIdentity.id()).stream().sorted(Comparator.comparing((RoomBooking b) -> b.dayOfWeek).thenComparing(b -> b.startTime)).map(BookingView::from).toList();
    }
    @Transactional
    public BookingView add(BookingInput input) {
        AvailabilityService.validateRange(input.startTime(), input.endTime());
        plans.requireCapacity("bookings");
        var room = rooms.findLockedById(input.roomId()).filter(r -> r.ownerId.equals(com.meetgrid.config.WorkspaceIdentity.id())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        if (input.startTime().isBefore(room.openTime) || input.endTime().isAfter(room.closeTime))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking must fit within the room's opening hours.");
        var candidate = Interval.of(input.startTime(), input.endTime());
        if (bookings.findByRoomIdAndDayOfWeek(room.id, input.dayOfWeek()).stream().anyMatch(b -> overlaps(candidate, Interval.of(b.startTime, b.endTime))))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This room already has a booking that overlaps that time.");
        var booking=new RoomBooking(room, input.dayOfWeek(), input.startTime(), input.endTime(), "user");
        details(booking,input);return BookingView.from(bookings.save(booking));
    }
    @Transactional
    public BookingView update(String id, BookingInput input) {
        AvailabilityService.validateRange(input.startTime(),input.endTime());plans.lockWorkspace();
        String owner=com.meetgrid.config.WorkspaceIdentity.id();
        var booking=bookings.findById(id).filter(b->b.room.ownerId.equals(owner)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found."));
        var locked=new HashMap<String,com.meetgrid.model.Room>();
        for(String roomId:new TreeSet<>(List.of(booking.room.id,input.roomId())))locked.put(roomId,rooms.findLockedById(roomId).filter(r->r.ownerId.equals(owner)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found.")));
        var room=locked.get(input.roomId());
        if(input.startTime().isBefore(room.openTime)||input.endTime().isAfter(room.closeTime))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Booking must fit within the room's opening hours.");
        var candidate=Interval.of(input.startTime(),input.endTime());
        if(bookings.findByRoomIdAndDayOfWeek(room.id,input.dayOfWeek()).stream().anyMatch(b->!b.id.equals(id)&&overlaps(candidate,Interval.of(b.startTime,b.endTime))))throw new ResponseStatusException(HttpStatus.CONFLICT,"This room already has an overlapping reservation. Your original booking is unchanged.");
        booking.room=room;booking.dayOfWeek=input.dayOfWeek();booking.startTime=input.startTime();booking.endTime=input.endTime();details(booking,input);
        return BookingView.from(bookings.save(booking));
    }
    private void details(RoomBooking booking,BookingInput input){booking.title=input.title()==null||input.title().isBlank()?"Team meeting":input.title().strip();booking.notes=input.notes()==null?"":input.notes().strip();}
}
