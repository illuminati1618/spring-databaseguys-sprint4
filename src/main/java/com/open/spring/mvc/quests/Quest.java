package com.open.spring.mvc.quests;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false, unique = true)
    private String permalink;

    @Column(nullable = false)
    private Integer totalSubmodules;

    @Column(nullable = false)
    private Integer rewardPoints;

    public Quest(String name, Difficulty difficulty, String permalink, Integer totalSubmodules, Integer rewardPoints) {
        this.name = name;
        this.difficulty = difficulty;
        this.permalink = permalink;
        this.totalSubmodules = totalSubmodules;
        this.rewardPoints = rewardPoints;
    }

    public Quest() {
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Difficulty getDifficulty() {
        return difficulty;
    }
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    public String getPermalink() {
        return permalink;
    }
    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }
    public Integer getTotalSubmodules() {
        return totalSubmodules;
    }
    public void setTotalSubmodules(Integer totalSubmodules) {
        this.totalSubmodules = totalSubmodules;
    }
    public Integer getRewardPoints() {
        return rewardPoints;
    }
    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
