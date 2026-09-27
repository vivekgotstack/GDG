package com.meetgrid.repository;
import com.meetgrid.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MemberRepository extends JpaRepository<Member, String> {}
