package com.open.spring.mvc.grades;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GraderModuleRepository extends JpaRepository<GraderModule, Long> {
    List<GraderModule> findByGraderId(String graderId);
    boolean existsByGraderIdAndAssignment(String graderId, String assignment);
}
