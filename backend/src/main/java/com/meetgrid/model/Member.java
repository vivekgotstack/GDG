package com.meetgrid.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
public class Member {
    @Column(nullable=false,length=40) public String ownerId = "legacy";
    @Id @Column(length = 40) public String id;
    @Column(nullable = false, length = 80) public String name;
    @Column(nullable = false, length = 20) public String color;
    @ElementCollection
    @CollectionTable(name = "availability", joinColumns = @JoinColumn(name = "member_id"))
    public List<Availability> availability = new ArrayList<>();

    protected Member() {}
    public Member(String id, String name, String color, List<Availability> availability) {
        this.id = id; this.name = name; this.color = color;
        this.availability = new ArrayList<>(availability);
    }
}
