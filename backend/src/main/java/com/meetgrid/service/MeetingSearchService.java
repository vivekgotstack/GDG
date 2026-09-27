package com.meetgrid.service;

import com.meetgrid.dto.Api.*;
import com.meetgrid.model.*;
import com.meetgrid.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.meetgrid.service.Intervals.*;

@Service
public class MeetingSearchService {
    private final MemberRepository members;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final AvailabilityService availability;
    private final RoomMatchingService matching;
    public MeetingSearchService(MemberRepository members, RoomRepository rooms, BookingRepository bookings,
        AvailabilityService availability, RoomMatchingService matching) {
        this.members = members; this.rooms = rooms; this.bookings = bookings; this.availability = availability; this.matching = matching;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SearchResponse search(SearchRequest request) {
        Set<String> ids = new HashSet<>(request.memberIds());
        if (ids.size() != request.memberIds().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose each member only once.");
        if (request.requiredCapacity() < ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room capacity must fit all selected members.");
        List<Member> group = members.findAllById(ids);
        if (group.size() != ids.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more members could not be found.");
        return findOptions(group, rooms.findAll(), bookings.findAll(), request.durationMinutes(), request.requiredCapacity());
    }
    public SearchResponse findOptions(List<Member> group, List<Room> rooms, List<RoomBooking> bookings, int duration, int capacity) {
        if (duration < 1) throw new IllegalArgumentException("Duration must be positive.");
        List<CommonInterval> common = availability.commonIntervals(group);
        List<MeetingOption> options = new ArrayList<>();
        int checked = 0, blocked = 0;
        for (CommonInterval interval : common) {
            int start = align(minutes(interval.startTime()));
            while (start + duration <= minutes(interval.endTime())) {
                checked++;
                var match = matching.match(rooms, bookings, interval.dayOfWeek(), time(start), time(start + duration), capacity);
                if (!match.available().isEmpty()) {
                    options.add(new MeetingOption(interval.dayOfWeek() + "-" + time(start), interval.dayOfWeek(), time(start), time(start + duration), match.available(), match.rejected()));
                    // Deduplicate only after matching: a blocked first slot must not hide a later viable start.
                    start = align(start + duration);
                } else { blocked++; start += 30; }
            }
        }
        return new SearchResponse(options, common, checked, blocked);
    }
}
