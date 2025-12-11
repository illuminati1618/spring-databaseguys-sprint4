package com.open.spring.mvc.trains;

import java.util.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
@Entity
@Convert(attributeName = "trainStation", converter = JsonType.class)
public class TrainStation {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JsonIgnore
    private TrainCompany company;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,List<Product>> products; //Product, amount available

    @Min(value=-10000)
    @Max(value=10000)
    private Float position;

    @Min(value=0)
    @Max(value=10)
    private Integer terrain; //terrain difficulty (0 easiest, 10 hardest)
}
