package com.open.spring.mvc.person;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.persistence.CascadeType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.open.spring.mvc.assignments.AssignmentSubmission;
import com.open.spring.mvc.bank.Bank;
import com.open.spring.mvc.bathroom.Tinkle;
import com.open.spring.mvc.groups.Groups;
import com.open.spring.mvc.groups.Submitter;
import com.open.spring.mvc.synergy.SynergyGrade;
import com.open.spring.mvc.trains.TrainCompany;
import com.open.spring.mvc.userStocks.userStocksTable;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;


/**
 * Person is a POJO, Plain Old Java Object.
 * --- @Data is Lombox annotation
 * for @Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor
 * --- @AllArgsConstructor is Lombox annotation for a constructor with all
 * arguments
 * --- @NoArgsConstructor is Lombox annotation for a constructor with no
 * arguments
 * --- @Entity annotation is used to mark the class as a persistent Java class.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonIgnoreProperties({"submissions", "groups"})
public class Person extends Submitter implements Comparable<Person> {

//////////////////////////////////////////////////////////////////////////////////
/// Columns stored on Person
    /** Automatic unique identifier for Person or group record 
     * --- Id annotation is used to specify the identifier property of the entity.
     * ----GeneratedValue annotation is used to specify the primary key generation
     * strategy to use.
     * ----- The strategy is to have the persistence provider pick an appropriate
     * strategy for the particular database.
     * ----- GenerationType.AUTO is the default generation type and it will pick the
     * strategy based on the used database.
     */
    // @Id
    // @GeneratedValue(strategy = GenerationType.AUTO)
    // private Long id;

    /**
     * email, password, roles are key attributes to login and authentication
     * --- @NotEmpty annotation is used to validate that the annotated field is not
     * null or empty, meaning it has to have a value.
     * --- @Size annotation is used to validate that the annotated field is between
     * the specified boundaries, in this case greater than 5.
     * --- @Email annotation is used to validate that the annotated field is a valid
     * email address.
     * --- @Column annotation is used to specify the mapped column for a persistent
     * property or field, in this case unique and email.
     */

    @NotEmpty
    @JsonIgnore
    private String password;

    @NotEmpty
    @Size(min = 1)
    @Column(unique = true, nullable = false)
    @Email
    private String email;


    @Column(unique = true, nullable = false)
    private String uid; // New `uid` column added

    /**
     * name, pfp attributes to describe the person
     * --- @NonNull annotation is used to generate a constructor witha
     * AllArgsConstructor Lombox annotation.
     * --- @Size annotation is used to validate that the annotated field is between
     * the specified boundaries, in this case between 2 and 30 characters.
     * --- @DateTimeFormat annotation is used to declare a field as a date, in this
     * case the pattern is specified as yyyy-MM-dd.
     */
    @NonNull
    @Size(min = 2, max = 30, message = "Name (2 to 30 chars)")
    private String name;




    /** Profile picture (pfp) in base64 */
    @Column(length = 255, nullable = true)
    private String pfp;


    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean kasmServerNeeded = false;

    @Column(nullable=true)
    private String sid;

    /**
     * stats is used to store JSON for daily stats
     * --- @JdbcTypeCode annotation is used to specify the JDBC type code for a
     * column, in this case json.
     * --- @Column annotation is used to specify the mapped column for a persistent
     * property or field, in this case columnDefinition is specified as jsonb.
     * * * Example of JSON data:
     * "stats": {
     * "2022-11-13": {
     * "calories": 2200,
     * "steps": 8000
     * }
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Map<String, Object>> stats = new HashMap<>();


//////////////////////////////////////////////////////////////////////////////////
/// Relationships


    @OneToMany(mappedBy="student", cascade=CascadeType.ALL, orphanRemoval=true)
    @JsonIgnore
    private List<SynergyGrade> grades;
    
 

    /**
     * Many to Many relationship with PersonRole
     * --- @ManyToMany annotation is used to specify a many-to-many relationship
     * between the entities.
     * --- FetchType.EAGER is used to specify that data must be eagerly fetched,
     * meaning that it must be loaded immediately.
     * --- Collection is a root interface in the Java Collection Framework, in this
     * case it is used to store PersonRole objects.
     * --- ArrayList is a resizable array implementation of the List interface,
     * allowing all elements to be accessed using an integer index.
     * --- PersonRole is a POJO, Plain Old Java Object.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<PersonRole> roles = new ArrayList<>();


    @OneToOne(mappedBy = "person", cascade=CascadeType.ALL)
    @JsonIgnore
    private Tinkle timeEntries;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "person")
    private Bank banks;


    @OneToOne(cascade = CascadeType.ALL, mappedBy = "person")
    @JsonIgnore
    private userStocksTable user_stocks;


    @ManyToMany(mappedBy = "groupMembers")
    @JsonBackReference
    @JsonIgnore
    private List<Groups> groups = new ArrayList<>();

    @OneToOne(mappedBy = "owner",  cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    @JsonIgnore
    private TrainCompany company;

//////////////////////////////////////////////////////////////////////////////////
/// Constructors


    /** Custom constructor for Person when building a new Person object from an API call
     * @param email, a String
     * @param password, a String
     * @param name, a String
     * @param dob, a Date
     */
    public Person(String email, String uid, String password, String sid, String name, String pfp, Boolean kasmServerNeeded, PersonRole role) {
        this.email = email;
        this.uid = uid;
        this.password = password;
        this.sid = sid;
        this.name = name;
        this.kasmServerNeeded = kasmServerNeeded;
        this.pfp = pfp;
        this.roles.add(role);

        this.timeEntries = new Tinkle(this, "");        
        // Create a Bank for this person
        this.banks = new Bank(this);
    }


    /** 1st telescoping method to create a Person object with USER role
     * @param name
     * @param email
     * @param password
     * @param dob
     * @return Person
     */
    public static Person createPerson(String name, String email, String uid, String password, String sid, Boolean kasmServerNeeded, List<String> asList) {
        // By default, Spring Security expects roles to have a "ROLE_" prefix.
        return createPerson(name, email, uid, password, sid, kasmServerNeeded, Arrays.asList("ROLE_USER", "ROLE_STUDENT"));
    }


    /**
     * 2nd telescoping method to create a Person object with parameterized roles
     * 
     * @param roles
     */
    public static Person createPerson(String name, String uid,  String email, String password, String sid,  String pfp, Boolean kasmServerNeeded, List<String> roleNames) {
        Person person = new Person();
        person.setName(name);
        person.setUid(uid);
        person.setEmail(email);
        person.setPassword(password);
        person.setSid(sid);
        person.setKasmServerNeeded(kasmServerNeeded);
        person.setPfp(pfp);
        List<PersonRole> roles = new ArrayList<>();
        for (String roleName : roleNames) {
            PersonRole role = new PersonRole(roleName);
            roles.add(role);
        }
        person.setRoles(roles);
        person.setBanks(new Bank(person));

        return person;
    }


//////////////////////////////////////////////////////////////////////////////////
/// getter methods


    /** Custom getter to return age from dob attribute
     * @return int, the age of the person
    */



//////////////////////////////////////////////////////////////////////////////////
    // other methods  
    
    /** Custom hasRoleWithName method to find if a role exists on user
     * @param roleName, a String with the name of the role
     * @return boolean, the result of the search
     */
    public boolean hasRoleWithName(String roleName) {
        for (PersonRole role : roles) {
            if (role.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }


    /** Custom compareTo method to compare Person objects by name
     * @param other, a Person object
     * @return int, the result of the comparison
     */
    @Override
    public int compareTo(Person other) {
        return this.name.compareTo(other.name);
    }


//////////////////////////////////////////////////////////////////////////////////
/// initalization method


    /**
     * Static method to initialize an array list of Person objects
     * Uses createPerson method to create Person objects
     * Sorts the list of Person objects using Collections.sort which uses the compareTo method 
     * @return Person[], an array of Person objects
     */
    public static Person[] init() {
        ArrayList<Person> people = new ArrayList<>();
        final Dotenv dotenv = Dotenv.load();
        final String adminPassword = dotenv.get("ADMIN_PASSWORD");
        final String defaultPassword = dotenv.get("DEFAULT_PASSWORD");
    
        // JSON-like list of person data using Map.ofEntries
        List<Map<String, Object>> personData = Arrays.asList(
            Map.ofEntries(
                Map.entry("name", "Thomas Edison"),
                Map.entry("uid", "toby"),
                Map.entry("email", "toby@gmail.com"),
                Map.entry("password", adminPassword),
                Map.entry("sid", "1"),
                Map.entry("pfp", "/images/toby.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_ADMIN", "ROLE_USER", "ROLE_TESTER", "ROLE_TEACHER")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "Alexander Graham Bell"),
                Map.entry("uid", "lex"),
                Map.entry("email", "lexb@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "1"),
                Map.entry("pfp", "/images/lex.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_USER", "ROLE_STUDENT")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "Nikola Tesla"),
                Map.entry("uid", "niko"),
                Map.entry("email", "niko@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "1"),
                Map.entry("pfp", "/images/niko.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_USER", "ROLE_STUDENT")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "Madam Curie"),
                Map.entry("uid", "madam"),
                Map.entry("email", "madam@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "1"),
                Map.entry("pfp", "/images/madam.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_USER", "ROLE_STUDENT")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "Grace Hopper"),
                Map.entry("uid", "hop"),
                Map.entry("email", "hop@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "123"),
                Map.entry("pfp", "/images/hop.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_USER", "ROLE_STUDENT")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "John Mortensen"),
                Map.entry("uid", "jm1021"),
                Map.entry("email", "jmort1021@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "1"),
                Map.entry("pfp", "/images/jm1021.png"),
                Map.entry("kasmServerNeeded", true),
                Map.entry("roles", Arrays.asList("ROLE_ADMIN", "ROLE_TEACHER")),
                Map.entry("stocks", "BTC,ETH")
            ),
            Map.ofEntries(
                Map.entry("name", "Alan Turing"),
                Map.entry("uid", "alan"),
                Map.entry("email", "turing@gmail.com"),
                Map.entry("password", defaultPassword),
                Map.entry("sid", "2"),
                Map.entry("pfp", "/images/alan.png"),
                Map.entry("kasmServerNeeded", false),
                Map.entry("roles", Arrays.asList("ROLE_USER", "ROLE_TESTER", "ROLE_STUDENT")),
                Map.entry("stocks", "BTC,ETH")
            )
        );
    
        // Iterate over the JSON-like list to create Person objects
        for (Map<String, Object> data : personData) {
            Person person = createPerson(
                (String) data.get("name"),
                (String) data.get("uid"),
                (String) data.get("email"),
                (String) data.get("password"),
                (String) data.get("sid"),
                (String) data.get("pfp"),
                (Boolean) data.get("kasmServerNeeded"),
                (List<String>) data.get("roles")
            );
            
            
            // Create userStocksTable and set the one-to-one relationship
            userStocksTable stock = new userStocksTable(
                null,
                (String) data.get("stocks"),
                person.getEmail(),
                person,
                false,
                true,
                ""
            );
            stock.setPerson(person); // Set the one-to-one relationship
            person.setUser_stocks(stock);
    
            people.add(person);
        }
    
        // Sort the list of people
        Collections.sort(people);
    
        return people.toArray(new Person[0]);
    }


//////////////////////////////////////////////////////////////////////////////////
/// override toString() method


    @Override
    public String toString(){
        String output = "person : {";
        output += "\"id\":"+ String.valueOf(this.getId())+","; //id
        output += "\"uid\":\""+ String.valueOf(this.getUid())+"\","; //user id (github/email)
        output += "\"email\":\""+ String.valueOf(this.getEmail())+"\","; //email
        // output += "\"password\":\""+ String.valueOf(this.getPassword())+"\","; //password
        output += "\"name\":\""+ String.valueOf(this.getName())+"\","; // name
        output += "\"sid\":\""+ String.valueOf(this.getSid())+"\","; // student id
        output += "\"kasmServerNeeded\":\""+ String.valueOf(this.getKasmServerNeeded())+"\","; // kasm server needed
        output += "\"stats\":"+ String.valueOf(this.getStats())+","; //stats (I think this is unused)
        output += "}";

        return output;
    }


//////////////////////////////////////////////////////////////////////////////////
/// public static void main(String[] args){}


    /**
     * Static method to print Person objects from an array
     * 
     * @param args, not used
     */
    public static void main(String[] args) {
        // obtain Person from initializer
        Person[] persons = init();

        // iterate using "enhanced for loop"
        for (Person person : persons) {
            System.out.println(person);  // print object
            System.out.println();
        }
    }

    @JsonIgnore
    public List<Groups> getGroups() {
        return groups;
    }

    public static List<AssignmentSubmission> getAllSubmissions(Person person) {
        // gets all the individual submissions and also the group submissions
        List<AssignmentSubmission> all = new ArrayList<>(person.getSubmissions());
        for (Groups group : person.getGroups()) {
            all.addAll(group.getSubmissions());
        }
        return all;
    }
}