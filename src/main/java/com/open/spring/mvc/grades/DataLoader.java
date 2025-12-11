package com.open.spring.mvc.grades;

import com.open.spring.mvc.grades.Grade;
import com.open.spring.mvc.grades.Progress;
import com.open.spring.mvc.grades.GradeRepository;
import com.open.spring.mvc.grades.ProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Override
    public void run(String... args) throws Exception {
        // Sample Grades
        gradeRepository.save(new Grade("STU001", "Mathematics", 85.5, "A", "submission link"));
        gradeRepository.save(new Grade("STU001", "Science", 92.0, "A+", "submission link"));
        gradeRepository.save(new Grade("STU002", "Mathematics", 78.0, "B+", "submission link"));
        gradeRepository.save(new Grade("STU002", "English", 88.5, "A-", "submission link"));
        gradeRepository.save(new Grade("STU003", "Science", 95.0, "A+", "submission link"));

        // Sample Progress
        progressRepository.save(new Progress("STU001", "Mathematics", 75.0, "In Progress"));
        progressRepository.save(new Progress("STU001", "Science", 100.0, "Completed"));
        progressRepository.save(new Progress("STU002", "Mathematics", 60.0, "In Progress"));
        progressRepository.save(new Progress("STU002", "English", 90.0, "Completed"));
        progressRepository.save(new Progress("STU003", "Science", 45.0, "In Progress"));

        System.out.println("Sample data loaded successfully!");
    }
}
