package com.open.spring.mvc.grades;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @Autowired
    private GradeRepository gradeRepository;

    // --- NEW ---
    @Autowired
    private GraderModuleRepository graderModuleRepository;

    // --- NEW: DTO for dashboard cards ---
    public static class ModuleCardDto {
        public String assignment;
        public long totalSubmissions;
        public long gradedCount;
        public long pendingCount;

        public ModuleCardDto(String assignment, long totalSubmissions, long gradedCount, long pendingCount) {
            this.assignment = assignment;
            this.totalSubmissions = totalSubmissions;
            this.gradedCount = gradedCount;
            this.pendingCount = pendingCount;
        }
    }
    // --- END NEW DTO ---

    @GetMapping
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grade> getGradeById(@PathVariable Long id) {
        Optional<Grade> grade = gradeRepository.findById(id);
        return grade.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{uid}")
    public List<Grade> getGradesByStudent(@PathVariable String uid) {
        return gradeRepository.findByUid(uid);
    }

    @GetMapping("/assignment/{assignment}")
    public List<Grade> getGradesByAssignment(@PathVariable String assignment) {
        return gradeRepository.findByAssignment(assignment);
    }

    @PostMapping
    public Grade createGrade(@RequestBody Grade grade) {
        return gradeRepository.save(grade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> updateGrade(@PathVariable Long id, @RequestBody Grade gradeDetails) {
        Optional<Grade> optionalGrade = gradeRepository.findById(id);
        if (optionalGrade.isPresent()) {
            Grade grade = optionalGrade.get();
            grade.setUid(gradeDetails.getUid());
            grade.setAssignment(gradeDetails.getAssignment());
            grade.setScore(gradeDetails.getScore());
            grade.setCourse(gradeDetails.getCourse());
            // grade.setGradeLevel(gradeDetails.getGradeLevel());
            return ResponseEntity.ok(gradeRepository.save(grade));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        if (gradeRepository.existsById(id)) {
            gradeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    // -------------------------------------------------------------------------
    // --- NEW: Grader-module endpoints ----------------------------------------
    // -------------------------------------------------------------------------

    // --- NEW ---
    // Grader selects a module (assignment) to grade
    @PostMapping("/modules/signup")
    public ResponseEntity<?> signupForModule(
            @RequestParam String graderId,
            @RequestParam String assignment) {

        if (graderModuleRepository.existsByGraderIdAndAssignment(graderId, assignment)) {
            return ResponseEntity.badRequest().body("Already signed up for this module");
        }

        GraderModule gm = new GraderModule(graderId, assignment);
        graderModuleRepository.save(gm);

        return ResponseEntity.ok("Signup successful");
    }
    // --- END NEW ---


    // --- NEW ---
    // Dashboard showing all modules this grader is assigned to
    @GetMapping("/modules/dashboard")
    public ResponseEntity<List<ModuleCardDto>> getDashboard(@RequestParam String graderId) {

        List<GraderModule> modules = graderModuleRepository.findByGraderId(graderId);

        List<ModuleCardDto> cards = modules.stream().map(m -> {
            String assignment = m.getAssignment();

            List<Grade> gradesForModule = gradeRepository.findByAssignment(assignment);

            long total = gradesForModule.size();
            long graded = gradesForModule.stream()
                    .filter(g -> g.getScore() != null)
                    .count();
            long pending = total - graded;

            return new ModuleCardDto(assignment, total, graded, pending);
        }).toList();

        return ResponseEntity.ok(cards);
    }
    // --- END NEW ---


    // --- NEW ---
    // View all submissions for a module (used by the grading UI)
    @GetMapping("/modules/{assignment}")
    public ResponseEntity<List<Grade>> getGradesForModule(@PathVariable String assignment) {

        List<Grade> grades = gradeRepository.findByAssignment(assignment);
        return ResponseEntity.ok(grades);
    }
    // --- END NEW ---

}
