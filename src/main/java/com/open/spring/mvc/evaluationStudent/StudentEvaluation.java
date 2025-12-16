package com.open.spring.mvc.evaluationStudent;

import jakarta.persistence.Column;
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
@Entity(name = "evaluation_student")
public class StudentEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Double attendance;

    @Column(nullable = false)
    private Double workHabits;

    @Column(nullable = false)
    private Double behavior;

    @Column(nullable = false)
    private Double timeliness;

    @Column(nullable = false)
    private Double techSense;

    @Column(nullable = false)
    private Double techTalk;

    @Column(nullable = false)
    private Double techGrowth;

    @Column(nullable = false)
    private Double advocacy;

    @Column(nullable = false)
    private Double communication;

    @Column(nullable = false)
    private Double integrity;

    @Column(nullable = false)
    private Double organization;

    @Column(nullable = false, updatable = false)
    private Long createdAt;

    public StudentEvaluation(Integer userId, Double attendance, Double workHabits, Double behavior, Double timeliness, Double techSense, Double techTalk, Double techGrowth, Double advocacy, Double communication, Double integrity, Double organization) {
        this.userId = userId;
        this.attendance = attendance;
        this.workHabits = workHabits;
        this.behavior = behavior;
        this.timeliness = timeliness;
        this.techSense = techSense;
        this.techTalk = techTalk;
        this.techGrowth = techGrowth;
        this.advocacy = advocacy;
        this.communication = communication;
        this.integrity = integrity;
        this.organization = organization;
        this.createdAt = System.currentTimeMillis();
    }
}
