package com.meetgrid.service;

import com.meetgrid.model.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

public final class WorkspaceReports {
 private WorkspaceReports(){}
 public record RoomSummary(String id,String name,String location,int capacity,int bookings,int reservedMinutes,int openingMinutes,double utilization){}
 public record RoomDay(String roomId,String roomName,String day,int bookings,int reservedMinutes,double utilization,int longestFreeMinutes,String freeFrom,String freeUntil){}
 public record Report(Instant generatedAt,List<RoomSummary> rooms,List<RoomDay> days,int reservedMinutes,int openingMinutes,String busiestDay){}
 private static int minute(LocalTime value){return value.getHour()*60+value.getMinute();}
 private static double percent(int used,int available){return available>0?Math.round(1000.0*used/available)/10.0:0;}
 public static Report calculate(List<Room> rooms,List<RoomBooking> bookings,boolean detailed){
  var summaries=new ArrayList<RoomSummary>();var days=new ArrayList<RoomDay>();var daily=new EnumMap<Weekday,Integer>(Weekday.class);int totalUsed=0,totalOpen=0;
  var byRoom=new HashMap<String,List<RoomBooking>>();for(var booking:bookings)byRoom.computeIfAbsent(booking.room.id,k->new ArrayList<>()).add(booking);
  for(var room:rooms){
   int open=minute(room.openTime),close=minute(room.closeTime),used=0;var reservations=byRoom.getOrDefault(room.id,List.of());
   for(var day:Weekday.values()){
    var intervals=reservations.stream().filter(b->b.dayOfWeek==day).sorted(Comparator.comparing(b->b.startTime)).toList();
    int cursor=open,dayUsed=0,bestStart=open,bestEnd=open;
    for(var b:intervals){int start=Math.max(open,minute(b.startTime)),end=Math.min(close,minute(b.endTime));if(end<=start)continue;
     if(start>cursor&&start-cursor>bestEnd-bestStart){bestStart=cursor;bestEnd=start;}
     dayUsed+=Math.max(0,end-Math.max(cursor,start));cursor=Math.max(cursor,end);
    }
    if(close-cursor>bestEnd-bestStart){bestStart=cursor;bestEnd=close;}
    used+=dayUsed;daily.merge(day,dayUsed,Integer::sum);
    if(detailed)days.add(new RoomDay(room.id,room.name,day.name(),intervals.size(),dayUsed,percent(dayUsed,close-open),bestEnd-bestStart,LocalTime.of(bestStart/60,bestStart%60).toString(),LocalTime.of(bestEnd/60,bestEnd%60).toString()));
   }
   int available=Math.max(0,close-open)*7;totalUsed+=used;totalOpen+=available;
   summaries.add(new RoomSummary(room.id,room.name,room.location,room.capacity,reservations.size(),used,available,percent(used,available)));
  }
  summaries.sort(Comparator.comparingDouble(RoomSummary::utilization).reversed().thenComparing(RoomSummary::name));
  String busiest=Arrays.stream(Weekday.values()).max(Comparator.comparingInt(day->daily.getOrDefault(day,0))).filter(day->daily.getOrDefault(day,0)>0).map(Enum::name).orElse("");
  return new Report(Instant.now(),summaries,days,totalUsed,totalOpen,busiest);
 }
}
