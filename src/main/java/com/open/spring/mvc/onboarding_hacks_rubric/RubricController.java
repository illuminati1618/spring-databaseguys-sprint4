package com.open.spring.mvc.onboarding_hacks_rubric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rubrics")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class RubricController {

    @Autowired
    private RubricRepository rubricRepository;

    /**
     * Get rubric for a specific student and assignment
     * GET /api/rubrics?uid=STU001&assignment=hack1
     */
    @GetMapping
    public ResponseEntity<?> getRubric(
            @RequestParam String uid,
            @RequestParam String assignment) {
        
        Optional<Rubric> rubric = rubricRepository.findByUidAndAssignment(uid, assignment);
        
        if (rubric.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", rubric.get().getId());
            response.put("uid", rubric.get().getUid());
            response.put("assignment", rubric.get().getAssignment());
            response.put("rubric", rubric.get().getRubricJson());
            response.put("updatedAt", rubric.get().getUpdatedAt());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all rubrics for a student
     * GET /api/rubrics/student/STU001
     */
    @GetMapping("/student/{uid}")
    public ResponseEntity<List<Rubric>> getRubricsByStudent(@PathVariable String uid) {
        List<Rubric> rubrics = rubricRepository.findByUid(uid);
        return ResponseEntity.ok(rubrics);
    }

    /**
     * Get all rubrics for an assignment
     * GET /api/rubrics/assignment/hack1
     */
    @GetMapping("/assignment/{assignment}")
    public ResponseEntity<List<Rubric>> getRubricsByAssignment(@PathVariable String assignment) {
        List<Rubric> rubrics = rubricRepository.findByAssignment(assignment);
        return ResponseEntity.ok(rubrics);
    }

    /**
     * Save or update a rubric
     * POST /api/rubrics
     * Body: { "uid": "STU001", "assignment": "hack1", "rubric": "{...}" }
     */
    @PostMapping
    public ResponseEntity<?> saveRubric(@RequestBody Map<String, String> request) {
        String uid = request.get("uid");
        String assignment = request.get("assignment");
        String rubricJson = request.get("rubric");

        if (uid == null || assignment == null || rubricJson == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Missing required fields: uid, assignment, rubric");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if rubric already exists
        Optional<Rubric> existingRubric = rubricRepository.findByUidAndAssignment(uid, assignment);

        Rubric rubric;
        if (existingRubric.isPresent()) {
            // Update existing rubric
            rubric = existingRubric.get();
            rubric.setRubricJson(rubricJson);
        } else {
            // Create new rubric
            rubric = new Rubric(uid, assignment, rubricJson);
        }

        Rubric savedRubric = rubricRepository.save(rubric);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rubric saved successfully");
        response.put("id", savedRubric.getId());
        response.put("uid", savedRubric.getUid());
        response.put("assignment", savedRubric.getAssignment());
        response.put("updatedAt", savedRubric.getUpdatedAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a rubric
     * DELETE /api/rubrics/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRubric(@PathVariable Long id) {
        if (rubricRepository.existsById(id)) {
            rubricRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Rubric deleted successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "rubric-service");
        return ResponseEntity.ok(response);
    }
}