package com.meetgrid.dto;

import com.meetgrid.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;

public final class Api {
    private Api() {}
    public record MemberInput(@NotBlank @Size(max = 80) String name,
        @NotNull @Pattern(regexp = "sage|lavender|apricot|blue|pink") String color) {}
    public record RoomInput(@NotBlank @Size(max = 100) String name,
        @Min(1) @Max(1000) int capacity, @NotNull LocalTime openTime, @NotNull LocalTime closeTime,
        @NotBlank @Size(max = 100) String location) {}
    public record AvailabilityInput(@NotNull Weekday dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record AvailabilityUpdate(@NotNull @Size(max = 100) List<@NotNull @Valid AvailabilityInput> availability) {}
    public record MemberView(String id, String name, String color, List<AvailabilityInput> availability) {
        public static MemberView from(Member m) {
            return new MemberView(m.id, m.name, m.color, m.availability.stream()
                .map(a -> new AvailabilityInput(a.dayOfWeek, a.startTime, a.endTime)).toList());
        }
    }
    public record RoomView(String id, String name, int capacity, LocalTime openTime, LocalTime closeTime, String location) {
        public static RoomView from(Room r) { return new RoomView(r.id, r.name, r.capacity, r.openTime, r.closeTime, r.location); }
    }
    public record BookingInput(@NotBlank String roomId, @NotNull Weekday dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record BookingView(String id, String roomId, String roomName, Weekday dayOfWeek, LocalTime startTime, LocalTime endTime, String source) {
        public static BookingView from(RoomBooking b) { return new BookingView(b.id, b.room.id, b.room.name, b.dayOfWeek, b.startTime, b.endTime, b.source); }
    }
    public record SearchRequest(@NotEmpty @Size(max = 50) List<@NotBlank String> memberIds,
        @Min(15) @Max(480) int durationMinutes, @Min(1) @Max(1000) int requiredCapacity) {}
    public record CommonInterval(Weekday dayOfWeek, LocalTime startTime, LocalTime endTime) {}
    public record RejectedRoom(RoomView room, List<String> reasons) {}
    public record MeetingOption(String id, Weekday dayOfWeek, LocalTime startTime, LocalTime endTime,
        List<RoomView> availableRooms, List<RejectedRoom> rejectedRooms) {}
    public record SearchResponse(List<MeetingOption> options, List<CommonInterval> commonIntervals,
        int checkedCandidates, int blockedCandidates) {}
}
