package com.open.spring.mvc.certificate;

import com.open.spring.mvc.quests.Quest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Date dateCreated;

    @ManyToMany(targetEntity = Quest.class)
    private List<Quest> certificateQuests;

    public Certificate() {
        this.dateCreated = new Date();
        this.certificateQuests = new ArrayList<>();
    }

    public Certificate(String title, List<Quest> certificateQuests) {
        this.dateCreated = new Date();
        this.title = title;
        this.certificateQuests = certificateQuests;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
 
    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public List<Quest> getCertificateQuests() {
        return certificateQuests;
    }

    public void setCertificateQuests(List<Quest> certificateQuests) {
        this.certificateQuests = certificateQuests;
    }
}
