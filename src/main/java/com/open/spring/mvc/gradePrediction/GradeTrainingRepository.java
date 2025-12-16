package com.open.spring.mvc.gradePrediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeTrainingRepository extends JpaRepository<GradeTraining, Long> {
    // This interface is intentionally left blank. 
    // Default JPA methods are used for database operations.
}
