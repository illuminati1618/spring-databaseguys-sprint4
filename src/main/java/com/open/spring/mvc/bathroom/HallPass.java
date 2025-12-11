package com.open.spring.mvc.bathroom;

import java.util.HashMap;
import java.util.Map;
import java.sql.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Convert;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vladmihalcea.hibernate.type.json.JsonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Teacher is a POJO, Plain Old Java Object.
 * --- @Data is Lombox annotation
 * for @Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor
 * --- @AllArgsConstructor is Lombox annotation for a constructor with all
 * arguments
 * --- @NoArgsConstructor is Lombox annotation for a constructor with no
 * arguments
 * --- @Entity annotation is used to mark the class as a persistent Java class.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Convert(attributeName = "hallpass", converter = JsonType.class)
public class HallPass {

    // Add a transient Teacher field (not stored in DB)
    @Transient
    private Teacher teacher;

    // Getter and Setter for Teacher
    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public long getTeacherId() {
        return this.teacher_id;
    }

    /**
     * automatic unique identifier for Teacher record
     * --- Id annotation is used to specify the identifier property of the entity.
     * ----GeneratedValue annotation is used to specify the primary key generation
     * strategy to use.
     * ----- The strategy is to have the persistence provider pick an appropriate
     * strategy for the particular database.
     * ----- GenerationType.AUTO is the default generation type and it will pick the
     * strategy based on the used database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * name, dob are attributes to describe the teacher
     * --- @NonNull annotation is used to generate a constructor with
     * AllArgsConstructor Lombox annotation.
     * --- @Size annotation is used to validate that the annotated field is between
     * the specified boundaries, in this case between 2 and 30 characters.
     * --- @DateTimeFormat annotation is used to declare a field as a date, in this
     * case the pattern is specified as yyyy-MM-dd.
     */

    private String personId;
    private long teacher_id;
    private int period;
    private String activity;
    private Date checkout;
    private Date checkin;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Map<String, Object>> stats = new HashMap<>();

    /**
     * Custom constructor for Teacher when building a new Teacher object from an API
     * call
     */
    public HallPass(int period, String activity, Date checkin, Date checkout) {
        this.period = period;
        this.activity = activity;
        this.checkin = checkin;
        this.checkout = checkout;
    }

    /**
     * Static method to print Teacher objects from an array
     * 
     * @param args, not used
     */
    public static void main(String[] args) {

    }

}