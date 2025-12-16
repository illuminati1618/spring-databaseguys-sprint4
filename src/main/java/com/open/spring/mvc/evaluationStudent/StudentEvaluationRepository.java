package com.open.spring.mvc.evaluationStudent;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentEvaluationRepository extends JpaRepository<StudentEvaluation, Long> {
    // This interface is intentionally left blank. 
    // Default JPA methods are used for database operations.
    Optional<StudentEvaluation> findByUserId(Integer userId);
}
