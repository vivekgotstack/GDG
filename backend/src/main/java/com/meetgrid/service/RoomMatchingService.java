package com.meetgrid.service;

import com.meetgrid.dto.Api.*;
import com.meetgrid.model.*;
import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static com.meetgrid.service.Intervals.*;

@Service
public class RoomMatchingService {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    public record Match(List<RoomView> available, List<RejectedRoom> rejected) {}

    public Match match(List<Room> rooms, List<RoomBooking> bookings, Weekday day, LocalTime start, LocalTime end, int capacity) {
        List<RoomView> available = new ArrayList<>();
        List<RejectedRoom> rejected = new ArrayList<>();
        Interval candidate = Interval.of(start, end);
        for (Room room : rooms.stream().sorted(Comparator.comparingInt((Room r) -> r.capacity).thenComparing(r -> r.name)).toList()) {
            List<String> reasons = new ArrayList<>();
            if (room.capacity < capacity) reasons.add("Seats " + room.capacity + "; " + capacity + " needed.");
            if (start.isBefore(room.openTime)) reasons.add("Opens at " + room.openTime.format(CLOCK) + ".");
            if (end.isAfter(room.closeTime)) reasons.add("Closes at " + room.closeTime.format(CLOCK) + ".");
            bookings.stream().filter(b -> b.room.id.equals(room.id) && b.dayOfWeek == day)
                .filter(b -> overlaps(candidate, Interval.of(b.startTime, b.endTime)))
                .sorted(Comparator.comparing(b -> b.startTime))
                .forEach(b -> reasons.add("Booked " + b.startTime.format(CLOCK) + " – " + b.endTime.format(CLOCK) + "."));
            if (reasons.isEmpty()) available.add(RoomView.from(room));
            else rejected.add(new RejectedRoom(RoomView.from(room), reasons));
        }
        return new Match(available, rejected);
    }
}
