package com.meetgrid.repository;
import com.meetgrid.model.PlanDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PlanRepository extends JpaRepository<PlanDefinition,String> {}
