package com.meetgrid.repository;
import com.meetgrid.model.RoomBooking;
import com.meetgrid.model.Weekday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BookingRepository extends JpaRepository<RoomBooking, String> {
    List<RoomBooking> findByRoomIdAndDayOfWeek(String roomId, Weekday dayOfWeek);
}
