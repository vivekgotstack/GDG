package com.meetgrid.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Embeddable
public class Availability {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    public Weekday dayOfWeek;
    @Column(nullable = false)
    public LocalTime startTime;
    @Column(nullable = false)
    public LocalTime endTime;

    protected Availability() {}
    public Availability(Weekday dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek; this.startTime = startTime; this.endTime = endTime;
    }
}
