package com.meetgrid.model;
import jakarta.persistence.*;
@Entity @Table(name="accounts")
public class Account {
 @Id @Column(length=40) public String id;
 @Column(nullable=false,unique=true,length=254) public String email;
 @Column(nullable=false,length=100) public String passwordHash;
 @Column(nullable=false,length=80) public String displayName;
 @Column(nullable=false,length=100) public String workspaceName;
 @Column(nullable=false,length=80) public String timezone;
 @Column(nullable=false,length=20) public String plan="free";
 @Column(length=100) public String stripeCustomer;
 @Column(length=100) public String subscriptionId;
 @Column(nullable=false) public long billingEventTime;
 @Column(nullable=false,length=20) public String role="USER";
 @Column(nullable=false) public boolean suspended=false;
}
