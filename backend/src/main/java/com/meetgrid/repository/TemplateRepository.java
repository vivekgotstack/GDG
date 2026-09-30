package com.meetgrid.repository;
import com.meetgrid.model.MeetingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TemplateRepository extends JpaRepository<MeetingTemplate,String> {
 long countByOwnerId(String ownerId);
 List<MeetingTemplate> findByOwnerId(String ownerId);
 Optional<MeetingTemplate> findByIdAndOwnerId(String id,String ownerId);
}
