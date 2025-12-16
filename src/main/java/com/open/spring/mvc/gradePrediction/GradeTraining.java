package com.open.spring.mvc.gradePrediction;

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
public class GradeTraining {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Double attendance;
    private Double workHabits;
    private Double behavior;
    private Double timeliness;
    private Double techSense;
    private Double techTalk;
    private Double techGrowth;
    private Double advocacy;
    private Double communication;
    private Double integrity;
    private Double organization;
    private Double finalGrade;
}
