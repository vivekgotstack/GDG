package com.meetgrid.model;
import jakarta.persistence.*;
@Entity @Table(name="site_content")
public class SiteContent {
 @Id @Column(length=30) public String id;
 @Column(nullable=false,columnDefinition="TEXT") public String payload;
 @Version public long version;
}
