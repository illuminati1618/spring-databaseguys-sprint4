package com.open.spring.mvc.bathroom;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vladmihalcea.hibernate.type.json.JsonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Teacher is a POJO, Plain Old Java Object.
 * --- @Data is Lombox annotation for @Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor
 * --- @AllArgsConstructor is Lombox annotation for a constructor with all arguments
 * --- @NoArgsConstructor is Lombox annotation for a constructor with no arguments
 * --- @Entity annotation is used to mark the class as a persistent Java class.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Convert(attributeName ="teacher", converter = JsonType.class)
public class Teacher {

    /** automatic unique identifier for Teacher record
     * --- Id annotation is used to specify the identifier property of the entity.
     * ----GeneratedValue annotation is used to specify the primary key generation strategy to use.
     * ----- The strategy is to have the persistence provider pick an appropriate strategy for the particular database.
     * ----- GenerationType.AUTO is the default generation type and it will pick the strategy based on the used database.
     */ 
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** name, dob are attributes to describe the teacher
     * --- @NonNull annotation is used to generate a constructor with AllArgsConstructor Lombox annotation.
     * --- @Size annotation is used to validate that the annotated field is between the specified boundaries, in this case between 2 and 30 characters.
     * --- @DateTimeFormat annotation is used to declare a field as a date, in this case the pattern is specified as yyyy-MM-dd.
     */ 
    @NonNull
    @Size(min = 2, max = 30, message = "First Name (2 to 30 chars)")
    private String firstname;

    @NonNull
    @Size(min = 2, max = 30, message = "Last Name (2 to 30 chars)")
    private String lastname;
    
        /** stats is used to store JSON for daily stat$
         * --- @JdbcTypeCode annotation is used to specify the JDBC type code for a column, in this case json.
         * --- @Column annotation is used to specify the mapped column for a persistent property or field, in this case columnDefinition is specified as jsonb.
         * * * Example of JSON data:
            "stats": {
                "2022-11-13": {
                    "calories": 2200,
                    "steps": 8000
                }
            }
        */
        @JdbcTypeCode(SqlTypes.JSON)
        private Map<String,Map<String, Object>> stats = new HashMap<>(); 
        
    
        /** Custom constructor for Teacher when building a new Teacher object from an API call
         */
        public Teacher(String firstname, String lastname) {
            this.firstname = firstname;
            this.lastname = lastname;
        }
    
    
        /** 1st telescoping method to create a Teacher object with USER role
         * @param first_name
         * @param last_name
         *  */ 
        public static Teacher createTeacher(String firstname, String lastname) {
            // By default, Spring Security expects roles to have a "ROLE_" prefix.
            Teacher teacher = new Teacher();
            teacher.setFirstname(firstname);
            teacher.setLastname(lastname);
        return teacher;
    }

   
    /** Static method to initialize an array list of Teacher objects 
     * @return Teacher[], an array of Teacher objects
     */
    public static ArrayList<Teacher> init() {
        ArrayList<Teacher> teachers = new ArrayList<>();
        teachers.add(createTeacher("Jason", "Askegreen"));
        teachers.add(createTeacher("Leigh", "Cole"));
        teachers.add(createTeacher("Amy", "Poynter-Jean"));
        teachers.add(createTeacher("Patrick", "Coleman"));
        teachers.add(createTeacher("Emily", "Pratt"));
        teachers.add(createTeacher("Jennie", "Ganesan"));
        teachers.add(createTeacher("Mike", "Giaime"));
        teachers.add(createTeacher("Valerie", "White"));
        teachers.add(createTeacher("Leslie", "Sullivan"));
        teachers.add(createTeacher("Amanda", "Waters"));
        teachers.add(createTeacher("Savannah", "Lentz"));
        teachers.add(createTeacher("Paul", "Mauer"));
        teachers.add(createTeacher("Shirley", "Rowe"));
        teachers.add(createTeacher("Deyanira", "Wilson"));
        teachers.add(createTeacher("Michael", "Buxton"));
        teachers.add(createTeacher("John", "Campillo"));
        teachers.add(createTeacher("Kris", "Palouda"));
        teachers.add(createTeacher("Cassandra", "Schaeg"));
        teachers.add(createTeacher("Carol", "Ferguson"));
        teachers.add(createTeacher("Scott", "Hughley"));
        teachers.add(createTeacher("Scott", "Huizar"));
        teachers.add(createTeacher("Craig", "Myrom"));
        teachers.add(createTeacher("Alexander", "Penarejo"));
        teachers.add(createTeacher("Ruth", "Wong"));
        teachers.add(createTeacher("Jeffrey", "Brown"));
        teachers.add(createTeacher("John", "Mortensen"));
        teachers.add(createTeacher("Bethany", "Watson"));
        teachers.add(createTeacher("Susie", "Kihneman"));
        teachers.add(createTeacher("Shaun", "Harvey"));
        teachers.add(createTeacher("Tim", "Roty"));
        teachers.add(createTeacher("Lauren", "Kennedy"));
        teachers.add(createTeacher("Jesse", "Luna"));
        teachers.add(createTeacher("Brenda", "Stone"));
        teachers.add(createTeacher("Kathleen", "Marron"));
        teachers.add(createTeacher("Michael", "Martinez"));
        teachers.add(createTeacher("Lauren", "DeCaprio"));
        teachers.add(createTeacher("Stephanie", "DeYoung"));
        teachers.add(createTeacher("Tien-Chi", "Chen"));
        teachers.add(createTeacher("Josie", "Boehm"));
        teachers.add(createTeacher("Stephanie", "Dafoe"));
        teachers.add(createTeacher("Melissa", "Darcey Hall"));
        teachers.add(createTeacher("Trent", "Hall"));
        teachers.add(createTeacher("Cara", "Jenkins"));
        teachers.add(createTeacher("Colleen", "Mansour"));
        teachers.add(createTeacher("Nick", "Nevares"));
        teachers.add(createTeacher("Erin", "Persil"));
        teachers.add(createTeacher("Jennifer", "Philyaw"));
        teachers.add(createTeacher("Rachel", "Ross"));
        teachers.add(createTeacher("Robert", "Weeg"));
        teachers.add(createTeacher("Ted", "West"));
        teachers.add(createTeacher("Robin", "Christopher"));
        teachers.add(createTeacher("Kristen", "James"));
        teachers.add(createTeacher("Kelly", "Paulson"));
        teachers.add(createTeacher("Rielly", "Embrey"));
        teachers.add(createTeacher("Kara", "Hanes"));
        teachers.add(createTeacher("Dale", "Hanover"));
        teachers.add(createTeacher("Brianna", "Kabaci"));
        teachers.add(createTeacher("Jake", "McNeely"));
        teachers.add(createTeacher("Suzanne", "Milanovich"));
        teachers.add(createTeacher("Kelley", "Lerner"));
        teachers.add(createTeacher("Arelli", "Caballero"));
        teachers.add(createTeacher("Miguel", "Lopez"));
        teachers.add(createTeacher("Trevor", "Cunningham"));
        teachers.add(createTeacher("Brad", "Zenz"));
        teachers.add(createTeacher("Susan", "Reich"));
        teachers.add(createTeacher("Danny", "Jilka"));
        teachers.add(createTeacher("Michelle", "Furtado"));
        teachers.add(createTeacher("Karla", "Palomino"));
        teachers.add(createTeacher("Joel", "Bernabeo"));
        teachers.add(createTeacher("Jo", "Buehler"));
        teachers.add(createTeacher("Austin", "Conery"));
        teachers.add(createTeacher("Scott", "DePutron"));
        teachers.add(createTeacher("Michelle", "Derksen"));
        teachers.add(createTeacher("Scott", "Edelstein"));
        teachers.add(createTeacher("Riley", "Froom"));
        teachers.add(createTeacher("Reanna", "Hightower"));
        teachers.add(createTeacher("Yali", "Huang"));
        teachers.add(createTeacher("Chris", "Jenkins"));
        teachers.add(createTeacher("Michelle", "Kim"));
        teachers.add(createTeacher("Jim", "Krenz"));
        teachers.add(createTeacher("James", "Lafferty"));
        teachers.add(createTeacher("Erika", "Larsen"));
        teachers.add(createTeacher("Michelle", "Lanzi-Sheaman"));
        teachers.add(createTeacher("Cherie", "Nydam"));
        teachers.add(createTeacher("Alfred", "Pangilinan"));
        teachers.add(createTeacher("Elizabeth", "Fierro"));
        teachers.add(createTeacher("Devin", "Hayworth"));
        teachers.add(createTeacher("Jennifer", "Kitelinger"));
        teachers.add(createTeacher("Carrie", "McCluskey"));
        teachers.add(createTeacher("Mara", "Nacu"));
        teachers.add(createTeacher("Ty", "Eveleth"));
        teachers.add(createTeacher("Shari", "Smith"));
        teachers.add(createTeacher("Kristina", "Call"));
        teachers.add(createTeacher("Alex", "Coughlin"));
        teachers.add(createTeacher("Sarah", "Olivarria"));
        teachers.add(createTeacher("Lanette", "Poulin"));
        teachers.add(createTeacher("Mariana", "Iniguez"));
        teachers.add(createTeacher("Kaitlyn", "Barron"));
        teachers.add(createTeacher("Amy", "Brunolli"));
        teachers.add(createTeacher("Jessica", "Camerino"));
        teachers.add(createTeacher("Nicole", "Crepeau"));
        teachers.add(createTeacher("Lauren", "Felker"));
        teachers.add(createTeacher("Aleena", "Khan"));
        teachers.add(createTeacher("Christian", "Neiland"));
        teachers.add(createTeacher("Elizabeth", "Parmley"));
        teachers.add(createTeacher("Scott", "Santos"));
        teachers.add(createTeacher("Joe", "South"));
        teachers.add(createTeacher("Melissa", "South"));
        teachers.add(createTeacher("Amy", "Summers"));
        teachers.add(createTeacher("Mudita", "Tiwary"));
        teachers.add(createTeacher("Nicholas", "Vonderhaar"));
        teachers.add(createTeacher("Hailey", "Stewart"));
        teachers.add(createTeacher("Christine", "Mikolosko"));
        teachers.add(createTeacher("Ashley", "Barnett"));
        teachers.add(createTeacher("Andrea", "Callicott"));
        teachers.add(createTeacher("Juli", "Cheskaty"));
        teachers.add(createTeacher("Courtney", "Craig"));
        teachers.add(createTeacher("David", "Dyer"));
        teachers.add(createTeacher("Kyle", "Eckman"));
        teachers.add(createTeacher("James", "Gusich"));
        teachers.add(createTeacher("Jay", "Hendricks"));
        teachers.add(createTeacher("Frank", "Liao"));
        teachers.add(createTeacher("Ryan", "Millman"));
        teachers.add(createTeacher("Tyler", "Moulton"));
        teachers.add(createTeacher("Ken", "Ozuna"));
        teachers.add(createTeacher("Emalia", "Pedraza"));
        teachers.add(createTeacher("Alaina", "Skelley"));
        teachers.add(createTeacher("Lisa", "Smedley"));
        teachers.add(createTeacher("Beth", "Byrd"));
        teachers.add(createTeacher("Gwendoline", "Olivares"));
        teachers.add(createTeacher("Lisa", "Stuber"));
        teachers.add(createTeacher("Elizabeth", "Ayres"));
        teachers.add(createTeacher("Scott", "Coats"));
        teachers.add(createTeacher("Neal", "Curry"));
        teachers.add(createTeacher("Denise", "Dupas"));
        teachers.add(createTeacher("Tasha", "Giffin"));
        teachers.add(createTeacher("Teresa", "Kim"));
        teachers.add(createTeacher("Ananda", "Rivera"));
        teachers.add(createTeacher("Jodi", "Roberts"));
        teachers.add(createTeacher("Charmaine", "Smith"));
        teachers.add(createTeacher("Joan", "Smith"));
        teachers.add(createTeacher("Tom", "Swanson"));
        teachers.add(createTeacher("Megan", "Volger"));
        teachers.add(createTeacher("Johnny", "Segovia"));
        teachers.add(createTeacher("Kenneth", "Simmons"));
        teachers.add(createTeacher("Daniel", "Park"));
        teachers.add(createTeacher("Kathy", "Wolff"));
        teachers.add(createTeacher("Michelle", "Castro"));
        teachers.add(createTeacher("Parisa", "Amini"));
        teachers.add(createTeacher("Maritza", "Balderas"));
        teachers.add(createTeacher("Holly", "Lecakes-Jones"));
        teachers.add(createTeacher("Christine", "Lin"));
        teachers.add(createTeacher("Leigh", "Murrell"));
        teachers.add(createTeacher("Lauren", "Ruggiero"));
        teachers.add(createTeacher("Aaron", "Strutton"));
        teachers.add(createTeacher("Leonardo", "Velasco"));

        return teachers;
    }

    /** Static method to print Teacher objects from an array
     * @param args, not used
     */
    public static void main(String[] args) {
        // obtain Teacher from initializer
        ArrayList<Teacher> teachers = init();

        // iterate using "enhanced for loop"
        for( Teacher teacher : teachers) {
            System.out.println(teacher);  // print object
        }
    }


    public void setEmail(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setEmail'");
    }

}