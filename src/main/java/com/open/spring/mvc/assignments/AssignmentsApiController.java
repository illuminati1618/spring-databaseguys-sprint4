package com.open.spring.mvc.assignments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.open.spring.mvc.groups.GroupsJpaRepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentsApiController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GroupsJpaRepository groupRepo;

    @Autowired
    private AssignmentJpaRepository assignmentRepo;

    @Autowired
    private AssignmentSubmissionJPA submissionRepo;

    @Autowired
    private PersonJpaRepository personRepo;

    @Getter
    @Setter
    public static class AssignmentDto {
        public Long id;
        public String name;
        public String type;
        public String description;
        public Double points;
        public String dueDate;
        public String timestamp;

        public AssignmentDto(Assignment assignment) {
            this.id = assignment.getId();
            this.name = assignment.getName();
            this.type = assignment.getType();
            this.description = assignment.getDescription();
            this.points = assignment.getPoints();
            this.dueDate = assignment.getDueDate();
            this.timestamp = assignment.getTimestamp();
        }
    }

    
    @Getter
    @Setter
    public static class PersonSubmissionDto {
        public Long id;
        public String name;
        public String email;
        public String uid;

        public PersonSubmissionDto(Person person) {
            this.id = person.getId();
            this.name = person.getName();
            this.email = person.getEmail();
            this.uid = person.getUid();
        }
    }

    /**
     * A POST endpoint to create an assignment, accepts parametes as FormData.
     * @param name The name of the assignment.
     * @param type The type of assignment.
     * @param description The description of the assignment.
     * @param points The amount of points the assignment is worth.
     * @param dueDate The due date of the assignment, in MM/DD/YYYY format.
     * @return The saved assignment.
     */
    @PostMapping("/create") 
    public ResponseEntity<?> createAssignment(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam Double points,
            @RequestParam String dueDate
    ) {
        Assignment newAssignment = new Assignment(name, type, description, points, dueDate);
        Assignment savedAssignment = assignmentRepo.save(newAssignment);
        return new ResponseEntity<>(savedAssignment, HttpStatus.CREATED);
    }

    /**
     * A GET endpoint to retrieve all the assignments.
     * @return A list of all the assignments.
     */
    @Transactional
    @GetMapping("/")
    public ResponseEntity<?> getAllAssignments() {
        List<Assignment> assignments = assignmentRepo.findAll();
        List<Map<String, String>> simple = new ArrayList<>();
        for (Assignment a : assignments) {
            Map<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(a.getId()));
            map.put("name", a.getName());
            map.put("description", a.getDescription());
            map.put("dueDate", a.getDueDate());
            map.put("points", String.valueOf(a.getPoints()));
            map.put("type", a.getType());
            simple.add(map);
        }
        return new ResponseEntity<>(simple, HttpStatus.OK);
    }

    /**
     * A POST endpoint to edit an assignment.
     * @param name The name of the assignment.
     * @param body The new information about the assignment.
     * @return The edited assignment.
     */
    @PostMapping("/edit/{name}")
    public ResponseEntity<?> editAssignment(
            @PathVariable String name,
            @RequestBody String body) {
        Assignment assignment = assignmentRepo.findByName(name);
        if (assignment != null) {
            assignment.setName(name);
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "Assignment not found: " + name);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * A GET endpoint to retrieve all submissions for a student.
     * @param studentId The ID of the student.
     * @return A list of all submissions for the student.
     * If the student is not found, returns a 404 error.
     * If the student has no submissions, returns an empty list.
     * If the student has submissions, returns a list of AssignmentSubmissionReturnDto objects.
     */
    @Transactional
    @GetMapping("/submissions/student/{studentId}")
    public ResponseEntity<?> getSubmissions(@PathVariable Long studentId) {
        if (personRepo.findById(studentId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }

        List<AssignmentSubmissionReturnDto> dtos = Stream.concat(
            submissionRepo.findBySubmitterId(studentId).stream(),
            groupRepo.findGroupsByPersonId(studentId).stream()
                .flatMap(group -> submissionRepo.findBySubmitterId(group.getId()).stream())
        )
        .map(AssignmentSubmissionReturnDto::new)
        .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * A GET endpoint to retrieve an assignment by its ID.
     * @param id The ID of the assignment.
     * @return The name of the assignment if found, or an error message if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Assignment> assignment = assignmentRepo.findById(id);

        if (assignment.isPresent()) {
            return ResponseEntity.ok(assignment.get().getName());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Assignment not found with ID: " + id);
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }


    /**
     * A POST endpoint to delete an assignment.
     * @param id The ID of the assignment to delete.
     * @return A JSON object indicating that the assignment was deleted.
     */
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Long id) {
        Assignment assignment = assignmentRepo.findById(id).orElse(null);
        if (assignment != null) {
            assignmentRepo.delete(assignment);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Assignment deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "Assignment not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * A GET endpoint used for debugging which returns information about every assignment.
     * @return Information about all the assignments.
     */
    @GetMapping("/debug")
    public ResponseEntity<?> debugAssignments() {
        List<Assignment> assignments = assignmentRepo.findAll();
        List<AssignmentDto> simple = new ArrayList<>();
        for (Assignment a : assignments) {
            simple.add(new AssignmentDto(a));
        }
        return new ResponseEntity<>(simple, HttpStatus.OK);
    }

    /**
     * A GET endpoint to retrieve all submissions for the assignment.
     * @param assignmentId The ID of the assignment.
     * @return All submissions for the assignment.
     */
    @Transactional
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<?> getSubmissions(@PathVariable Long assignmentId, @AuthenticationPrincipal UserDetails userDetails) {
        String uid = userDetails.getUsername();
        Person user = personRepo.findByUid(uid);

        if (user == null) {
            logger.error("User not found with email: {}", uid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with uid: " + uid);
        }

        Stream<AssignmentSubmission> submissions = submissionRepo.findByAssignmentId(assignmentId).stream();

        if (!(user.hasRoleWithName("ROLE_TEACHER") || user.hasRoleWithName("ROLE_ADMIN"))) {
            // if they aren't a teacher or admin, only let them see submissions they are assigned to grade
            submissions = submissions
                .filter(submission -> submission.getAssignedGraders().contains(user));
        }

        List<AssignmentSubmissionReturnDto> returnValue = submissions
            .map(AssignmentSubmissionReturnDto::new)
            .toList();

        return new ResponseEntity<>(returnValue, HttpStatus.OK);
    }

    /**
     * A GET endpoint to retrieve the queue for an assignment.
     * @param id The ID of the assignment.
     * @return Queue for assignment, formatted in JSON
     */
    @GetMapping("/getQueue/{id}")
    public ResponseEntity<AssignmentQueue> getQueue(@PathVariable long id) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            
            return new ResponseEntity<>(assignment.getAssignmentQueue(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A GET endpoint to retrieve the presentation length for an assignment.
     * @param id The ID of the assignment.
     * @return Presentation length for assignment, formatted in JSON
     */
    @GetMapping("/getPresentationLength/{id}")
    public ResponseEntity<Long> getPresentationLength(@PathVariable long id) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            
            return new ResponseEntity<>(assignment.getPresentationLength(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A PUT endpoint to initialize an empty queue for an assignment.
     * @param id The ID of the assignment.
     * @return Queue for assignment, formatted in JSON
     */
    @PutMapping("/initQueue/{id}")
    public ResponseEntity<Assignment> initQueue(@PathVariable long id, @RequestBody List<List<String>> people) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            assignment.initQueue(people.get(0), Long.parseLong(people.get(1).get(0)));
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A PUT endpoint to add a user to the waiting list
     * @param id The ID of the assignment.
     * @param person Name of person to be added to waiting list in one element array.
     * @return Updated queue for assignment, formatted in JSON
     */
    @PutMapping("/addToWaiting/{id}")
    public ResponseEntity<Assignment> addQueue(@PathVariable long id, @RequestBody List<String> person) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            assignment.addQueue(person.get(0));
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A PUT endpoint to return a user to the working list
     * @param id The ID of the assignment.
     * @param person Name of person to be returned to the working list in one element array.
     * @return Updated queue for assignment, formatted in JSON
     */
    @PutMapping("/removeToWorking/{id}")
    public ResponseEntity<Assignment> removeQueue(@PathVariable long id, @RequestBody List<String> person) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            assignment.removeQueue(person.get(0));
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A PUT endpoint to move a user to the completed list
     * @param id The ID of the assignment.
     * @param person Name of person to be moved to the completed list in one element array.
     * @return Updated queue for assignment, formatted in JSON
     */
    @PutMapping("/doneToCompleted/{id}")
    public ResponseEntity<Assignment> doneQueue(@PathVariable long id, @RequestBody List<String> person) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            assignment.doneQueue(person.get(0));
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A PUT endpoint to reset a queue to its empty form.
     * @param id The ID of the assignment.
     * @return Updated queue for assignment, formatted in JSON
     */
    @PutMapping("/resetQueue/{id}")
    public ResponseEntity<Assignment> resetQueue(@PathVariable long id) {
        Optional<Assignment> optional = assignmentRepo.findById(id);
        if (optional.isPresent()) {
            Assignment assignment = optional.get();
            assignment.resetQueue();
            assignmentRepo.save(assignment);
            return new ResponseEntity<>(assignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * A POST endpoint to assign graders to an assignment
     * @param id The ID of the assignment.
     * @param personIds A list of person IDs to be assigned as graders.
     * @return A response indicating success or failure.
     */
    @PostMapping("/assignGraders/{id}")
    public ResponseEntity<?> assignGradersToAssignment( @PathVariable Long id, @RequestBody List<Long> personIds ) {
        Optional<Assignment> assignmentOptional = assignmentRepo.findById(id);
        if (!assignmentOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Assignment not found");
        }

        Assignment assignment = assignmentOptional.get();
        List<Person> persons = personRepo.findAllById(personIds);

        assignment.setAssignedGraders(persons);

        assignmentRepo.save(assignment);

        return ResponseEntity.ok("Persons assigned successfully");
    }
    
    /**
     * A POST endpoint to sign up for a team teach assignment.
     * @param userDetails The authenticated user details.
     * @param id The ID of the assignment.
     * @return A response indicating success or failure.
     */
    @PostMapping("/teamteach/signup/{id}")
    @Transactional
    public ResponseEntity<?> signupForTeamteach(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You must be a logged in user to do this");
        }
        String uid = userDetails.getUsername();
        Person user = personRepo.findByUid(uid);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You must be a logged in user to do this");
        }
        
        Optional<Assignment> assignmentOptional = assignmentRepo.findById(id);
        if (!assignmentOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Assignment not found");
        }

        Assignment assignment = assignmentOptional.get();
        if (!assignment.getType().equals("teamteach")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This assignment is not a team teach assignment");
        }

        // Check if user is already assigned to the assignment
        if (assignment.getAssignedGraders().stream().anyMatch(assignedGrader -> assignedGrader.getId().equals(user.getId()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are already signed up for this team teach");
        }

        List<Person> assignedGraders = assignment.getAssignedGraders();
        if (assignedGraders == null) {
            assignedGraders = new ArrayList<>();
        }
        assignedGraders.add(user);
        assignment.setAssignedGraders(assignedGraders);
        assignmentRepo.save(assignment);

        return ResponseEntity.ok("You have successfully signed up for the team teach assignment");        
    }

    /**
     * A GET endpoint to retrieve the assigned graders for an assignment.
     * @param id The ID of the assignment.
     * @return A list of assigned graders for the assignment.
     */
    @GetMapping("/assignedGraders/{id}")
    @Transactional
    public ResponseEntity<?> getAssignedGraders(@PathVariable Long id) {
        Optional<Assignment> assignmentOptional = assignmentRepo.findById(id);
        if (!assignmentOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Assignment not found");
        }

        Assignment assignment = assignmentOptional.get();
        List<Person> assignedGraders = assignment.getAssignedGraders();
        
        // Return just the IDs of assigned persons
        List<PersonSubmissionDto> assignedGraderIds = assignedGraders.stream()
            .map(PersonSubmissionDto::new)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(assignedGraderIds);
    }
    
    /**
     * A GET endpoint to retrieve all assignments that the logged-in user is assigned to grade.
     * @param userDetails The authenticated user details.
     * @return A list of AssignmentDto objects representing the assignments assigned to the user.
     * If the user is not logged in, returns a 404 error.
     */
    @Transactional
    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String uid = userDetails.getUsername();
        Person user = personRepo.findByUid(uid);
        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a logged in user to do this"
            );
        }

        List<Assignment> assignments = assignmentRepo.findByAssignedGraders(user);
        List<AssignmentSubmission> submissions = submissionRepo.findByAssignedGraders(user);

        List<AssignmentDto> formattedAssignments = new ArrayList<>();
        for (Assignment a : assignments) {
            formattedAssignments.add(new AssignmentDto(a));
        }
        for (AssignmentSubmission s: submissions) {
            formattedAssignments.add(new AssignmentDto(s.getAssignment()));
        }

        return ResponseEntity.ok(formattedAssignments);
    }

    /**
     * A GET endpoint to bulk extract all assignments for backups.
     * @return A list of AssignmentDto objects representing all assignments.
     */
    @GetMapping("/bulk/extract")
    public ResponseEntity<List<AssignmentDto>> bulkExtractAssignments() {
        // Fetch all Assignment entities from the database
        List<Assignment> assignment = assignmentRepo.findAll();

        // Map Assignment entities to Assignment objects
        List<AssignmentDto> assignmentDtos = new ArrayList<>();
        for (Assignment assignment2 : assignment) {
            AssignmentDto assignmentDto = new AssignmentDto(assignment2);
            
            assignmentDtos.add(assignmentDto);
        }
        // Return the list of Assignment objects
        return new ResponseEntity<>(assignmentDtos, HttpStatus.OK);
    }

    /**
     * Bulk create Assignment entities from a list of AssignmentDto objects.
     * 
     * @param assignmentDtos A list of AssignmentDto objects to be created.
     * @return A ResponseEntity containing the result of the bulk creation.
     */
    @PostMapping("/bulk/create")
    public ResponseEntity<Object> bulkCreateAssignments(@RequestBody List<AssignmentDto> assignmentDtos) {
        List<String> createdAssignments = new ArrayList<>();
        List<String> duplicateAssignments = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (AssignmentDto assignmentDto : assignmentDtos) {
            try {
                // Check if assignment with the same name already exists
                Assignment existingAssignment = assignmentRepo.findByName(assignmentDto.name);
                if (existingAssignment != null) {
                    duplicateAssignments.add(assignmentDto.name);
                    continue;
                }
                
                // Create a new Assignment entity from the DTO
                Assignment newAssignment = new Assignment(
                    assignmentDto.name, 
                    assignmentDto.type, 
                    assignmentDto.description, 
                    assignmentDto.points, 
                    assignmentDto.dueDate
                );
                
                // Save the new assignment
                Assignment savedAssignment = assignmentRepo.save(newAssignment);
                createdAssignments.add(savedAssignment.getName());
                
            } catch (Exception e) {
                // Handle exceptions
                errors.add("Exception occurred for assignment: " + assignmentDto.name + " - " + e.getMessage());
            }
        }
        
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("created", createdAssignments);
        response.put("duplicates", duplicateAssignments);
        response.put("errors", errors);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * A POST endpoint to randomize peer graders for an assignment.
     * This method shuffles the submissions and assigns each submission a random grader from the pool of submissions.
     * @param id The ID of the assignment for which to randomize peer graders.
     * @return A response indicating success or failure.
     * If the assignment is not found, returns a 404 error.
     */
    @PostMapping("/randomizeGraders/{id}")
    @Transactional
    public ResponseEntity<?> randomizePeerGraders(@PathVariable Long id) {
        Optional<Assignment> assignmentOptional = assignmentRepo.findById(id);
        if (!assignmentOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Assignment not found");
        }

        List<AssignmentSubmission> submissions = submissionRepo.findByAssignmentId(id);
    
        if (submissions.isEmpty()) {
            return ResponseEntity.badRequest().body("No submissions found for this assignment");
        }
        
        if (submissions.size() == 1) {
            return ResponseEntity.badRequest().body("Only one submission found for this assignment, can't really do peer grading");
        }

        Collections.shuffle(submissions);

        for (int i = 0; i < submissions.size(); i++) {
            AssignmentSubmission currentSubmission = submissions.get(i);
            
            // grader whos not the asme persoon
            List<AssignmentSubmission> possibleGraders = submissions.stream()
                .filter(submission -> Collections.disjoint(
                    submission.getSubmitter().getMembers(), 
                    currentSubmission.getSubmitter().getMembers()
                )) // ensure no overlap between the members of the two groups
                .collect(Collectors.toList());
    
            if (possibleGraders.isEmpty()) {
                System.out.println("FATAL: No possible graders found for submission by: " + 
                    currentSubmission.getSubmitter().getMembers().stream()
                        .map(Person::getName)
                        .collect(Collectors.joining(", ")));

                // TODO: implement a better randomization strategy. In theory, it is possible that all submissions are from various groups which all include one same person, therefore causing this issue.
                continue; 
            }
    
            // Randomly select
            AssignmentSubmission graderSubmission = possibleGraders.get(
                (int)(Math.random() * possibleGraders.size())
            );
    
            // Assign graders to the current submission
            // Create a new list instead of sharing the existing one
            currentSubmission.setAssignedGraders(graderSubmission.getSubmitter().getMembers());
        }

    
        submissionRepo.saveAll(submissions);
        // test debug
        // for (AssignmentSubmission sub : uniqueSubmissions) {
        //     System.out.println("Submission by: " + sub.getStudents().get(0).getName() + 
        //                     " is graded by: " + 
        //                     sub.getAssignedGraders().stream()
        //                         .map(Person::getName)
        //                         .collect(Collectors.joining(", ")));
        // }
    
        return ResponseEntity.ok("Graders randomized successfully!");
    }


    /**
     * A GET endpoint to extract an assignment by its ID.
     * This endpoint retrieves the assignment details and returns them as an AssignmentDto object.
     * If the assignment is not found, it returns a 404 error.
     * @param id The ID of the assignment to extract.
     * @return A ResponseEntity containing the AssignmentDto object if found, or a 404 error if not found.
     */
    @GetMapping("/extract/{id}")
    public ResponseEntity<AssignmentDto> extractAssignment(@PathVariable Long id) {
        Optional<Assignment> curAssignment = assignmentRepo.findById(id);
        Assignment assignment = curAssignment.get();
        AssignmentDto assignmentDto = new AssignmentDto(assignment);
        return new ResponseEntity<>(assignmentDto, HttpStatus.OK);
    }
}