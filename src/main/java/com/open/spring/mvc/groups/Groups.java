package com.open.spring.mvc.groups;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.open.spring.mvc.person.Person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "groups")
@Getter
@Setter
public class Groups extends Submitter {
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "group_members", 
        joinColumns = @JoinColumn(name = "group_id"), 
        inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @JsonIgnore
    private List<Person> groupMembers = new ArrayList<>();

    private String name;
    private String period;

    public Groups() {
    }

    public Groups(String name, String period, List<Person> groupMembers) {
        this.name = name;
        this.period = period;
        this.groupMembers = groupMembers;
    }

    public void addPerson(Person person) {
        if (!this.groupMembers.contains(person)) {
            this.groupMembers.add(person);
            person.getGroups().add(this);
        }
    }

    public void removePerson(Person person) {
        if (this.groupMembers.contains(person)) {
            this.groupMembers.remove(person);
            person.getGroups().remove(this);
        }
    }
}
