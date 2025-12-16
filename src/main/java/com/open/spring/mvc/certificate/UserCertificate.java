package com.open.spring.mvc.certificate;

import java.util.Date;

import com.open.spring.mvc.person.Person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class UserCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(targetEntity = Person.class, optional = false)
    private Person person;

    @ManyToOne(targetEntity = Certificate.class, optional = false)
    private Certificate certificate;

    @Column(nullable = false)
    private Date dateIssued;

    public UserCertificate() {
        this.dateIssued = new Date();
    }

    public UserCertificate(Person person, Certificate certificate) {
        this.person = person;
        this.certificate = certificate;
        this.dateIssued = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Date getDateIssued() {
        return dateIssued;
    }

    public void setDateIssued(Date dateIssued) {
        this.dateIssued = dateIssued;
    }
}
