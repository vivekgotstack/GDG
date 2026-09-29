package com.meetgrid.repository;
import com.meetgrid.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface RoomRepository extends JpaRepository<Room, String> {
    java.util.List<Room> findByOwnerId(String ownerId);
    long countByOwnerId(String ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findLockedById(@Param("id") String id);
}
