package com.meetgrid.repository;
import com.meetgrid.model.AdminEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AdminEventRepository extends JpaRepository<AdminEvent,String> {
 List<AdminEvent> findTop100ByOrderByOccurredAtDesc();
}
