package com.meetgrid.controller;
import com.meetgrid.config.WorkspaceIdentity;
import com.meetgrid.repository.*;
import com.meetgrid.service.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tools")
public class ReportsController {
 private final PlanService plans;private final RoomRepository rooms;private final BookingRepository bookings;
 public ReportsController(PlanService p,RoomRepository r,BookingRepository b){plans=p;rooms=r;bookings=b;}
 @GetMapping("/insights") @Transactional(readOnly=true) public WorkspaceReports.Report insights(){plans.requireFeature("insights");String owner=WorkspaceIdentity.id();return WorkspaceReports.calculate(rooms.findByOwnerId(owner),bookings.findByRoomOwnerId(owner),false);}
 @GetMapping("/operations") @Transactional(readOnly=true) public WorkspaceReports.Report operations(){plans.requireFeature("operations_reports");String owner=WorkspaceIdentity.id();return WorkspaceReports.calculate(rooms.findByOwnerId(owner),bookings.findByRoomOwnerId(owner),true);}
}
