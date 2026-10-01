package com.meetgrid.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="payment_subscriptions")
public class PaymentSubscription {
 @Id @Column(length=100) public String id;
 @Column(nullable=false,length=40) public String ownerId;
 @Column(nullable=false,length=20) public String planId;
 @Column(nullable=false,length=100) public String providerPlanId;
 @Column(nullable=false,length=100) public String keyId;
 @Column(nullable=false) public int amount;
 @Column(nullable=false,length=3) public String currency;
 @Column(nullable=false,length=30) public String status="created";
 public Instant paidUntil;
 public Instant currentEnd;
 @Column(nullable=false) public int paidCount;
 @Column(nullable=false) public boolean cancelAtPeriodEnd;
 @Column(length=20) public String pendingPlanId;
 @Column(length=100) public String pendingProviderPlanId;
 public Integer pendingAmount;
 @Column(length=3) public String pendingCurrency;
 @Column(length=30) public String paymentMethod;
 @Column(length=100) public String lastPaymentId;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant syncedAt=Instant.now();
 public boolean hasAccess(Instant now){return paidUntil!=null&&paidUntil.isAfter(now);}
 public boolean terminal(){return java.util.Set.of("cancelled","completed","expired").contains(status);}
}
