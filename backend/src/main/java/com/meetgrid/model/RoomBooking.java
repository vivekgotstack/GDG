package com.meetgrid.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity @Table(name = "room_bookings")
public class RoomBooking {
    @Id @Column(length = 40) public String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    public Room room;
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    public Weekday dayOfWeek;
    @Column(nullable = false) public LocalTime startTime;
    @Column(nullable = false) public LocalTime endTime;
    @Column(nullable = false, length = 20) public String source;

    protected RoomBooking() {}
    public RoomBooking(Room room, Weekday day, LocalTime start, LocalTime end, String source) {
        this.id = UUID.randomUUID().toString(); this.room = room; this.dayOfWeek = day;
        this.startTime = start; this.endTime = end; this.source = source;
    }
}
