package com.open.spring.mvc.academicProgress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcademicPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("assignment_completion_rate")
    private Double assignmentCompletionRate;
    @JsonProperty("average_assignment_score")
    private Double averageAssignmentScore;
    @JsonProperty("collegeboard_quiz_average")
    private Double collegeboardQuizAverage;
    @JsonProperty("office_hours_visits")
    private Integer officeHoursVisits;
    private Integer conduct;
    @JsonProperty("work_habit")
    private Integer workHabit;
    @JsonProperty("github_contributions")
    private Integer githubContributions;
    @JsonProperty("final_grade")
    private Double predictedScore;
    private Long createdAt;

    public AcademicPrediction(Double assignmentCompletionRate, Double averageAssignmentScore, 
                            Double collegeboardQuizAverage, Integer officeHoursVisits, 
                            Integer conduct, Integer workHabit, Integer githubContributions, 
                            Double predictedScore) {
        this.assignmentCompletionRate = assignmentCompletionRate;
        this.averageAssignmentScore = averageAssignmentScore;
        this.collegeboardQuizAverage = collegeboardQuizAverage;
        this.officeHoursVisits = officeHoursVisits;
        this.conduct = conduct;
        this.workHabit = workHabit;
        this.githubContributions = githubContributions;
        this.predictedScore = predictedScore;
        this.createdAt = System.currentTimeMillis();
    }
}
