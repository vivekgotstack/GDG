package com.meetgrid.model;
import jakarta.persistence.*;
@Entity @Table(name="plan_definitions")
public class PlanDefinition {
 @Id @Column(length=20) public String id;
 @Column(nullable=false,length=80) public String name;
 @Column(nullable=false,length=300) public String description;
 @Column(nullable=false) public int monthlyPrice;
 @Column(nullable=false,length=3) public String currency;
 @Column(nullable=false) public int members;
 @Column(nullable=false) public int rooms;
 @Column(nullable=false) public int bookings;
 @Column(nullable=false) public int presets;
 @Version public long version;
}
