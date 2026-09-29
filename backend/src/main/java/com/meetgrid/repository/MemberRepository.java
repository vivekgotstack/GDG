package com.meetgrid.repository;
import com.meetgrid.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MemberRepository extends JpaRepository<Member, String> {
 java.util.List<Member> findByOwnerId(String ownerId);
 java.util.Optional<Member> findByIdAndOwnerId(String id,String ownerId);
 long countByOwnerId(String ownerId);
}
