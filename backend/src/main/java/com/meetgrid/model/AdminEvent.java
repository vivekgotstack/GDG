package com.meetgrid.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="admin_events")
public class AdminEvent {
 @Id @Column(length=40) public String id;
 @Column(nullable=false,length=40) public String actorId;
 @Column(nullable=false,length=100) public String action;
 @Column(nullable=false,length=100) public String target;
 @Column(nullable=false) public Instant occurredAt;
}
