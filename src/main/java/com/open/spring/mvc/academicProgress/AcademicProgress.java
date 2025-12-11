package com.open.spring.mvc.academicProgress;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AcademicProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Double assignmentCompletionRate;
    private Double averageAssignmentScore;
    private Double collegeboardQuizAverage;
    private Integer officeHoursVisits;
    private Integer conduct;
    private Integer workHabit;
    private Integer githubContributions;
    private Integer finalGrade;
}
