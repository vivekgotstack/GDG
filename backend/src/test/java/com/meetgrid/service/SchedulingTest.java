package com.meetgrid.service;

import com.meetgrid.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.*;
import static com.meetgrid.model.Weekday.*;
import static com.meetgrid.service.Intervals.*;
import static org.assertj.core.api.Assertions.*;

class SchedulingTest {
    private final AvailabilityService availability = new AvailabilityService(null);
    private final RoomMatchingService matching = new RoomMatchingService();
    private final MeetingSearchService search = new MeetingSearchService(null, null, null, availability, matching);
    private Member member(String id, String start, String end) {
        return new Member(id, id, "sage", List.of(new Availability(TUESDAY, LocalTime.parse(start), LocalTime.parse(end))));
    }
    private Room room(int capacity) { return new Room("r", "Test room", capacity, "09:00", "17:00", "Campus"); }
    private RoomBooking booking(Room r, String start, String end) {
        return new RoomBooking(r, TUESDAY, LocalTime.parse(start), LocalTime.parse(end), "demo");
    }
    @Test void fourMembersIntersectCorrectly() {
        var common = availability.commonIntervals(List.of(member("a","10:00","13:00"), member("b","11:00","14:00"),
            member("c","10:30","12:30"), member("d","11:00","15:00")));
        assertThat(common).hasSize(1);
        assertThat(common.getFirst().startTime()).isEqualTo(LocalTime.of(11,0));
        assertThat(common.getFirst().endTime()).isEqualTo(LocalTime.of(12,30));
    }
    @Test void unavailableMemberPreventsSlot() {
        assertThat(availability.commonIntervals(List.of(member("a","10:00","13:00"), new Member("b","b","blue",List.of())))).isEmpty();
    }
    @Test void touchingAvailabilityIsNotAnIntersection() {
        assertThat(availability.commonIntervals(List.of(member("a","10:00","11:00"),member("b","11:00","12:00")))).isEmpty();
    }
    @Test void multipleRangesAreMergedAndIntersectedWithoutDuplicates() {
        var a = member("a","09:00","10:30");
        a.availability.add(new Availability(TUESDAY,LocalTime.of(10,0),LocalTime.of(12,0)));
        a.availability.add(new Availability(TUESDAY,LocalTime.of(14,0),LocalTime.of(16,0)));
        var result = availability.commonIntervals(List.of(a,member("b","10:00","15:00")));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).endTime()).isEqualTo(LocalTime.NOON);
        assertThat(result.get(1).startTime()).isEqualTo(LocalTime.of(14,0));
    }
    @Test void adjacentAvailabilityCanSupportOneContinuousMeeting() {
        assertThat(normalize(List.of(new Interval(600,630),new Interval(630,660)))).containsExactly(new Interval(600,660));
    }
    @Test void durationMustFit() {
        assertThat(search.findOptions(List.of(member("a","10:00","10:45")),List.of(room(8)),List.of(),60,4).options()).isEmpty();
        assertThat(search.findOptions(List.of(member("a","10:00","11:00")),List.of(room(8)),List.of(),60,4).options()).hasSize(1);
    }
    @Test void capacityIsChecked() {
        var result = matching.match(List.of(room(3)),List.of(),TUESDAY,LocalTime.of(10,0),LocalTime.of(11,0),4);
        assertThat(result.available()).isEmpty();
        assertThat(result.rejected().getFirst().reasons()).contains("Seats 3; 4 needed.");
    }
    @Test void bookingConflictsAreRejected() {
        Room r = room(8);
        var result = matching.match(List.of(r),List.of(booking(r,"10:30","12:00")),TUESDAY,LocalTime.of(10,0),LocalTime.of(11,0),4);
        assertThat(result.available()).isEmpty();
        assertThat(result.rejected().getFirst().reasons().getFirst()).contains("10:30 AM");
    }
    @Test void adjacentBookingsDoNotOverlap() {
        Room r = room(8);
        assertThat(matching.match(List.of(r),List.of(booking(r,"09:00","10:00"),booking(r,"11:00","12:00")),
            TUESDAY,LocalTime.of(10,0),LocalTime.of(11,0),4).available()).hasSize(1);
    }
    @Test void exactBookingIsAnOverlapAndContainmentWorksBothWays() {
        assertThat(overlaps(new Interval(600,660),new Interval(600,660))).isTrue();
        assertThat(overlaps(new Interval(600,660),new Interval(615,630))).isTrue();
        assertThat(overlaps(new Interval(615,630),new Interval(600,660))).isTrue();
    }
    @Test void openingAndClosingBoundariesAreInclusive() {
        Room r = room(8);
        assertThat(matching.match(List.of(r),List.of(),TUESDAY,LocalTime.of(9,0),LocalTime.of(17,0),4).available()).hasSize(1);
        assertThat(matching.match(List.of(r),List.of(),TUESDAY,LocalTime.of(8,30),LocalTime.of(9,30),4).available()).isEmpty();
        assertThat(matching.match(List.of(r),List.of(),TUESDAY,LocalTime.of(16,30),LocalTime.of(17,30),4).available()).isEmpty();
    }
    @Test void conflictsOnOtherDaysDoNotBlock() {
        Room r=room(8); RoomBooking b=booking(r,"10:00","11:00"); b.dayOfWeek=MONDAY;
        assertThat(matching.match(List.of(r),List.of(b),TUESDAY,LocalTime.of(10,0),LocalTime.of(11,0),4).available()).hasSize(1);
    }
    @Test void addingBookingRemovesOptionWithNoRooms() {
        Room r=room(8); var group=List.of(member("a","10:00","11:00"));
        assertThat(search.findOptions(group,List.of(r),List.of(),60,4).options()).hasSize(1);
        assertThat(search.findOptions(group,List.of(r),List.of(booking(r,"10:00","11:00")),60,4).options()).isEmpty();
    }
    @Test void addingBookingKeepsOtherSuitableRoom() {
        Room first=room(8), second=new Room("r2","Second",6,"09:00","17:00","Campus");
        var result=search.findOptions(List.of(member("a","10:00","11:00")),List.of(first,second),List.of(booking(first,"10:00","11:00")),60,4);
        assertThat(result.options()).hasSize(1);
        assertThat(result.options().getFirst().availableRooms()).extracting(r->r.id()).containsExactly("r2");
    }
    @Test void startsAlignToHalfHourAndOptionsDoNotOverlap() {
        var result=search.findOptions(List.of(member("a","10:10","13:00")),List.of(room(8)),List.of(),60,4);
        assertThat(result.options()).hasSize(2);
        assertThat(result.options().getFirst().startTime()).isEqualTo(LocalTime.of(10,30));
        assertThat(result.options().getLast().startTime()).isEqualTo(LocalTime.of(11,30));
    }
    @Test void blockedEarlyCandidateDoesNotHideLaterViableStart() {
        Room r=room(8);
        var result=search.findOptions(List.of(member("a","10:00","11:30")),List.of(r),List.of(booking(r,"09:30","10:30")),60,4);
        assertThat(result.options()).hasSize(1);
        assertThat(result.options().getFirst().startTime()).isEqualTo(LocalTime.of(10,30));
        assertThat(result.blockedCandidates()).isEqualTo(1);
    }
}
