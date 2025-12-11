package com.open.spring.mvc.grades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grades")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private String assignment;

    @Column(nullable = true)
    private Double score;

    @Column(name = "class")
    @com.fasterxml.jackson.annotation.JsonProperty("class")
    private String course;

    @Column(nullable = true)
    private String submission;


    public Grade() {
    }

    public Grade(String uid, String assignment, Double score, String course, String submission) {
        this.uid = uid;
        this.assignment = assignment;
        this.score = score;
        this.course = course;
        this.submission = submission;
        // this.gradeLevel = gradeLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAssignment() {
        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSubmission() {
        return submission;
    }

    public void setSubmission(String submission) {
        this.submission = submission;
    }


}
