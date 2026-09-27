package com.meetgrid.service;

import com.meetgrid.model.*;
import com.meetgrid.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.*;
import static com.meetgrid.model.Weekday.*;

@Service
public class SeedService {
    private final MemberRepository members;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    public SeedService(MemberRepository members, RoomRepository rooms, BookingRepository bookings) {
        this.members = members; this.rooms = rooms; this.bookings = bookings;
    }
    @Transactional
    public void ensureSeeded() { if (members.count() == 0) seed(); }
    @Transactional
    public void reset() {
        bookings.deleteAll(); bookings.flush();
        members.deleteAll(); members.flush();
        rooms.deleteAll(); rooms.flush();
        seed();
    }
    private static Availability a(Weekday day, String start, String end) {
        return new Availability(day, LocalTime.parse(start), LocalTime.parse(end));
    }
    private void seed() {
        members.saveAll(List.of(
            new Member("1-vivek", "Vivek", "sage", List.of(a(MONDAY,"09:00","12:00"), a(TUESDAY,"13:00","16:00"), a(WEDNESDAY,"09:00","11:00"), a(THURSDAY,"10:00","13:00"), a(FRIDAY,"14:00","17:00"))),
            new Member("2-riya", "Riya", "lavender", List.of(a(MONDAY,"10:00","13:00"), a(TUESDAY,"14:00","17:00"), a(WEDNESDAY,"13:00","15:00"), a(THURSDAY,"11:00","14:00"), a(FRIDAY,"09:00","12:00"))),
            new Member("3-aman", "Aman", "apricot", List.of(a(MONDAY,"11:00","14:00"), a(TUESDAY,"12:00","15:00"), a(WEDNESDAY,"10:00","12:00"), a(THURSDAY,"09:00","12:00"), a(FRIDAY,"10:00","14:00"))),
            new Member("4-priya", "Priya", "blue", List.of(a(MONDAY,"15:00","17:00"), a(TUESDAY,"13:30","16:30"), a(WEDNESDAY,"14:00","17:00"), a(THURSDAY,"10:30","12:30"), a(FRIDAY,"13:00","16:00")))
        ));
        var seeded = rooms.saveAll(List.of(
            new Room("lab-2", "Lab 2", 8, "09:00", "18:00", "Innovation block · Floor 2"),
            new Room("discussion-a", "Discussion Room A", 6, "09:00", "17:00", "Library · Floor 1"),
            new Room("room-103", "Room 103", 3, "08:00", "18:00", "Main building · Floor 1"),
            new Room("seminar", "Seminar Hall", 24, "10:00", "18:00", "Academic block · Ground floor"),
            new Room("lab-1", "Lab 1", 12, "09:00", "12:00", "Innovation block · Floor 1")
        ));
        Map<String, Room> byId = new HashMap<>();
        seeded.forEach(r -> byId.put(r.id, r));
        bookings.saveAll(List.of(
            b(byId.get("discussion-a"), TUESDAY, "14:00", "15:00"),
            b(byId.get("seminar"), TUESDAY, "14:30", "16:00"),
            b(byId.get("seminar"), THURSDAY, "11:00", "13:00"),
            b(byId.get("lab-1"), THURSDAY, "10:00", "12:00")
        ));
    }
    private RoomBooking b(Room room, Weekday day, String start, String end) {
        return new RoomBooking(room, day, LocalTime.parse(start), LocalTime.parse(end), "seed");
    }
}
