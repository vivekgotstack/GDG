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
    public BookingService(RoomRepository rooms, BookingRepository bookings) { this.rooms = rooms; this.bookings = bookings; }
    @Transactional(readOnly = true)
    public List<BookingView> list() {
        return bookings.findAll().stream().sorted(Comparator.comparing((RoomBooking b) -> b.dayOfWeek).thenComparing(b -> b.startTime)).map(BookingView::from).toList();
    }
    @Transactional
    public BookingView add(BookingInput input) {
        AvailabilityService.validateRange(input.startTime(), input.endTime());
        var room = rooms.findLockedById(input.roomId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        if (input.startTime().isBefore(room.openTime) || input.endTime().isAfter(room.closeTime))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking must fit within the room's opening hours.");
        var candidate = Interval.of(input.startTime(), input.endTime());
        if (bookings.findByRoomIdAndDayOfWeek(room.id, input.dayOfWeek()).stream().anyMatch(b -> overlaps(candidate, Interval.of(b.startTime, b.endTime))))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This room already has a booking that overlaps that time.");
        return BookingView.from(bookings.save(new RoomBooking(room, input.dayOfWeek(), input.startTime(), input.endTime(), "demo")));
    }
}
