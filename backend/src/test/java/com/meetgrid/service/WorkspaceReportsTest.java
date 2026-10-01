package com.meetgrid.service;
import com.meetgrid.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class WorkspaceReportsTest {
 private final Room room=new Room("room","Main space",20,"09:00","17:00","North");
 private RoomBooking booking(String start,String end){return new RoomBooking(room,Weekday.MONDAY,LocalTime.parse(start),LocalTime.parse(end),"test");}
 @Test void overlappingReservationsCountOnceAndFreeWindowsAreAccurate(){
  var report=WorkspaceReports.calculate(List.of(room),List.of(booking("10:00","12:00"),booking("11:00","13:00"),booking("15:00","16:00")),true);
  var monday=report.days().getFirst();assertThat(monday.reservedMinutes()).isEqualTo(240);assertThat(monday.utilization()).isEqualTo(50);assertThat(monday.longestFreeMinutes()).isEqualTo(120);assertThat(monday.freeFrom()).isEqualTo("13:00");assertThat(monday.freeUntil()).isEqualTo("15:00");assertThat(report.reservedMinutes()).isEqualTo(240);assertThat(report.openingMinutes()).isEqualTo(3360);assertThat(report.busiestDay()).isEqualTo("MONDAY");assertThat(report.days()).hasSize(7);
 }
 @Test void intervalsAreClippedToOpeningHoursAndFullyReservedRoomHasNoGap(){
  var report=WorkspaceReports.calculate(List.of(room),List.of(booking("07:00","09:30"),booking("09:30","18:00"),booking("19:00","20:00")),true);
  assertThat(report.days().getFirst().reservedMinutes()).isEqualTo(480);assertThat(report.days().getFirst().utilization()).isEqualTo(100);assertThat(report.days().getFirst().longestFreeMinutes()).isZero();
 }
 @Test void emptyWorkspacesAndUnusedRoomsHaveUsefulReports(){
  var empty=WorkspaceReports.calculate(List.of(),List.of(),true);assertThat(empty.rooms()).isEmpty();assertThat(empty.busiestDay()).isEmpty();assertThat(empty.openingMinutes()).isZero();
  var report=WorkspaceReports.calculate(List.of(room),List.of(),true);assertThat(report.days()).allSatisfy(day->{assertThat(day.reservedMinutes()).isZero();assertThat(day.longestFreeMinutes()).isEqualTo(480);assertThat(day.freeFrom()).isEqualTo("09:00");assertThat(day.freeUntil()).isEqualTo("17:00");});
  assertThat(WorkspaceReports.calculate(List.of(room),List.of(),false).days()).isEmpty();
 }
 @Test void roomsStaySeparateAndSummariesAreOrderedByUtilization(){
  var unused=new Room("other","Empty room",4,"10:00","12:00","South");var report=WorkspaceReports.calculate(List.of(unused,room),List.of(booking("09:00","17:00")),true);
  assertThat(report.rooms().getFirst().id()).isEqualTo("room");assertThat(report.days().stream().filter(day->day.roomId().equals("other"))).allSatisfy(day->assertThat(day.reservedMinutes()).isZero());assertThat(report.openingMinutes()).isEqualTo(4200);
 }
}
