package com.meetgrid.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity @Table(name = "rooms")
public class Room {
    @Id @Column(length = 40) public String id;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false) public int capacity;
    @Column(nullable = false) public LocalTime openTime;
    @Column(nullable = false) public LocalTime closeTime;
    @Column(nullable = false, length = 100) public String location;

    protected Room() {}
    public Room(String id, String name, int capacity, String open, String close, String location) {
        this.id = id; this.name = name; this.capacity = capacity;
        this.openTime = LocalTime.parse(open); this.closeTime = LocalTime.parse(close); this.location = location;
    }
}
