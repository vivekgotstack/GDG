package com.meetgrid.repository;
import com.meetgrid.model.RoomBooking;
import com.meetgrid.model.Weekday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
public interface BookingRepository extends JpaRepository<RoomBooking, String> {
    @EntityGraph(attributePaths = "room")
    List<RoomBooking> findByRoomOwnerId(String ownerId);
    long countByRoomOwnerId(String ownerId);
    @Override @EntityGraph(attributePaths = "room")
    List<RoomBooking> findAll();
    @Override @EntityGraph(attributePaths = "room")
    Optional<RoomBooking> findById(String id);
    List<RoomBooking> findByRoomId(String roomId);
    List<RoomBooking> findByRoomIdAndDayOfWeek(String roomId, Weekday dayOfWeek);
}
