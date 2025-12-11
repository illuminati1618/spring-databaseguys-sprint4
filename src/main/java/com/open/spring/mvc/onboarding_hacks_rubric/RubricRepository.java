package com.open.spring.mvc.onboarding_hacks_rubric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, Long> {
    
    // Find rubric by student and assignment
    Optional<Rubric> findByUidAndAssignment(String uid, String assignment);
    
    // Find all rubrics for a student
    List<Rubric> findByUid(String uid);
    
    // Find all rubrics for an assignment
    List<Rubric> findByAssignment(String assignment);
}