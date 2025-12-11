package com.open.spring.mvc.trains;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.util.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Entity
@Data
@Convert(attributeName = "train", converter = JsonType.class)
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="company_id")
    private TrainCompany company;

    @Min(value=-10000)
    @Max(value=10000)
    private Float position;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,List<Product>> cargo; //Product, amount available

    
    private Boolean inTransit;

    private String trainType;

    private String trainColor;

    public static Train createTrain(){
        Train train = new Train();
        //default position of 0
        train.setPosition(Float.valueOf(0));
        //defalt no cargo
        train.setCargo(new HashMap<String,List<Product>>());
        //train does not start by moving
        train.setInTransit(false);
        //assign type of train
        train.setTrainType("trolley");
        //assign a random color for the train
        int random = (int)(Math.random()*7); //red orange yellow green blue purple
        switch (random) {
            case 0:
                train.setTrainColor("red");
                break;
            case 1:
                train.setTrainColor("orange");
                break;
            case 2:
                train.setTrainColor("yellow");
                break;
            case 3:
                train.setTrainColor("green");
                break;
            case 4:
                train.setTrainColor("blue");
                break;
            case 5:
                train.setTrainColor("purple");
                break;        
            default:
                train.setTrainColor("red");
                break;
        }

        //return the train, company needs to be assigned
        return train;
    }
}
