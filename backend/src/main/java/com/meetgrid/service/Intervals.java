package com.meetgrid.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Half-open intervals: [start, end). Adjacent intervals do not overlap. */
public final class Intervals {
    private Intervals() {}
    public record Interval(int start, int end) {
        public Interval {
            if (start < 0 || end > 1440 || start >= end) throw new IllegalArgumentException("Start must be before end within the same day.");
        }
        public static Interval of(LocalTime start, LocalTime end) { return new Interval(minutes(start), minutes(end)); }
    }
    public static int minutes(LocalTime t) { return t.getHour() * 60 + t.getMinute(); }
    public static LocalTime time(int minutes) { return LocalTime.of(minutes / 60, minutes % 60); }
    public static int align(int minutes) { return ((minutes + 29) / 30) * 30; }
    public static boolean overlaps(Interval a, Interval b) { return a.start < b.end && a.end > b.start; }
    public static List<Interval> normalize(List<Interval> ranges) {
        List<Interval> merged = new ArrayList<>();
        for (Interval next : ranges.stream().sorted(Comparator.comparingInt(Interval::start)).toList()) {
            if (merged.isEmpty() || next.start > merged.getLast().end) merged.add(next);
            else {
                Interval previous = merged.removeLast();
                merged.add(new Interval(previous.start, Math.max(previous.end, next.end)));
            }
        }
        return merged;
    }
    public static List<Interval> intersect(List<Interval> first, List<Interval> second) {
        List<Interval> a = normalize(first), b = normalize(second), result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            int start = Math.max(a.get(i).start, b.get(j).start), end = Math.min(a.get(i).end, b.get(j).end);
            if (start < end) result.add(new Interval(start, end));
            if (a.get(i).end < b.get(j).end) i++; else j++;
        }
        return result;
    }
}
