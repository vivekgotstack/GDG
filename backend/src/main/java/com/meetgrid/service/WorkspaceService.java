package com.meetgrid.service;

import com.meetgrid.dto.Api.*;
import com.meetgrid.model.*;
import com.meetgrid.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.meetgrid.config.WorkspaceIdentity.id;

@Service
@Transactional
public class WorkspaceService {
    private final MemberRepository members;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final PlanService plans;
    public WorkspaceService(MemberRepository members, RoomRepository rooms, BookingRepository bookings, PlanService plans) {
        this.plans = plans;
        this.members = members; this.rooms = rooms; this.bookings = bookings;
    }
    private ResponseStatusException missing(String kind) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, kind + " not found.");
    }
    public MemberView addMember(MemberInput input) {
        plans.requireCapacity("members");
        var member = new Member(UUID.randomUUID().toString(), input.name().strip(), input.color(), List.of());
        member.ownerId = id(); return MemberView.from(members.save(member));
    }
    public MemberView updateMember(String id, MemberInput input) {
        var member = members.findByIdAndOwnerId(id, id()).orElseThrow(() -> missing("Member"));
        member.name = input.name().strip(); member.color = input.color();
        return MemberView.from(member);
    }
    public void deleteMember(String id) {
        members.delete(members.findByIdAndOwnerId(id, id()).orElseThrow(() -> missing("Member")));
    }
    public RoomView saveRoom(String id, RoomInput input) {
        AvailabilityService.validateRange(input.openTime(), input.closeTime());
        Room room;
        if (id == null) {
            plans.requireCapacity("rooms");
            room = new Room(UUID.randomUUID().toString(), input.name().strip(), input.capacity(),
                input.openTime().toString(), input.closeTime().toString(), input.location().strip());
            room.ownerId = id();
        } else {
            room = rooms.findLockedById(id).filter(r -> r.ownerId.equals(id())).orElseThrow(() -> missing("Room"));
            if (bookings.findByRoomId(id).stream().anyMatch(b -> b.startTime.isBefore(input.openTime()) || b.endTime.isAfter(input.closeTime())))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing bookings fall outside these hours. Cancel those bookings first.");
            room.name = input.name().strip(); room.capacity = input.capacity();
            room.openTime = input.openTime(); room.closeTime = input.closeTime(); room.location = input.location().strip();
        }
        return RoomView.from(rooms.save(room));
    }
    public void deleteRoom(String id) {
        var room = rooms.findLockedById(id).filter(r -> r.ownerId.equals(id())).orElseThrow(() -> missing("Room"));
        bookings.deleteAll(bookings.findByRoomId(id));
        bookings.flush();
        rooms.delete(room);
    }
    public void cancelBooking(String id) {
        var booking = bookings.findById(id).filter(b -> b.room.ownerId.equals(id())).orElseThrow(() -> missing("Booking"));
        rooms.findLockedById(booking.room.id).orElseThrow(() -> missing("Room"));
        bookings.delete(booking);
    }
}
