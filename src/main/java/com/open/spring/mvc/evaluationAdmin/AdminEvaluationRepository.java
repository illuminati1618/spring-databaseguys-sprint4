package com.open.spring.mvc.evaluationAdmin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminEvaluationRepository extends JpaRepository<AdminEvaluation, Long> {
    // This interface is intentionally left blank. 
    // Default JPA methods are used for database operations.
    Optional<AdminEvaluation> findByUserId(Integer userId);
}
