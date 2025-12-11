package com.open.spring.mvc.grades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "grader_modules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"grader_id", "assignment"})
)
public class GraderModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grader_id", nullable = false)
    private String graderId;

    @Column(nullable = false)
    private String assignment;

    public GraderModule() {}

    public GraderModule(String graderId, String assignment) {
        this.graderId = graderId;
        this.assignment = assignment;
    }

    public Long getId() { return id; }
    public String getGraderId() { return graderId; }
    public String getAssignment() { return assignment; }

    public void setId(Long id) { this.id = id; }
    public void setGraderId(String graderId) { this.graderId = graderId; }
    public void setAssignment(String assignment) { this.assignment = assignment; }
}

