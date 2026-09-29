package com.meetgrid.model;
import jakarta.persistence.*;
@Entity @Table(name="meeting_templates")
public class MeetingTemplate {
 @Id @Column(length=40) public String id;
 @Column(nullable=false,length=40) public String ownerId;
 @Column(nullable=false,length=100) public String name;
 @Column(nullable=false,length=500) public String description;
 @Column(nullable=false) public int durationMinutes;
 @Column(nullable=false) public int capacity;
}
