package com.meetgrid.service;

import com.meetgrid.dto.Api.*;
import com.meetgrid.model.*;
import com.meetgrid.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.meetgrid.service.Intervals.*;

@Service
public class AvailabilityService {
    private final MemberRepository members;
    public AvailabilityService(MemberRepository members) { this.members = members; }

    @Transactional(readOnly = true)
    public List<MemberView> list() {
        return members.findByOwnerId(com.meetgrid.config.WorkspaceIdentity.id()).stream().sorted(Comparator.comparing(m -> m.name)).map(MemberView::from).toList();
    }
    @Transactional
    public MemberView update(String id, AvailabilityUpdate request) {
        Member m = members.findByIdAndOwnerId(id, com.meetgrid.config.WorkspaceIdentity.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));
        for (AvailabilityInput a : request.availability()) validateRange(a.startTime(), a.endTime());
        m.availability.clear();
        for (Weekday day : Weekday.values()) {
            List<Interval> normalized = normalize(request.availability().stream().filter(a -> a.dayOfWeek() == day)
                .map(a -> Interval.of(a.startTime(), a.endTime())).toList());
            for (Interval i : normalized) m.availability.add(new Availability(day, time(i.start()), time(i.end())));
        }
        return MemberView.from(members.save(m));
    }
    public static void validateRange(java.time.LocalTime start, java.time.LocalTime end) {
        if (!start.isBefore(end) || start.getSecond() != 0 || end.getSecond() != 0 || start.getNano() != 0 || end.getNano() != 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use whole-minute times with start before end on the same day.");
    }
    public List<CommonInterval> commonIntervals(List<Member> group) {
        if (group.isEmpty()) return List.of();
        List<CommonInterval> result = new ArrayList<>();
        for (Weekday day : Weekday.values()) {
            List<Interval> common = List.of(new Interval(0, 1440));
            for (Member member : group) {
                common = intersect(common, member.availability.stream().filter(a -> a.dayOfWeek == day)
                    .map(a -> Interval.of(a.startTime, a.endTime)).toList());
                if (common.isEmpty()) break;
            }
            for (Interval i : common) result.add(new CommonInterval(day, time(i.start()), time(i.end())));
        }
        return result;
    }
}
