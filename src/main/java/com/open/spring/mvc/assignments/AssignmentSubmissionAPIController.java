package com.open.spring.mvc.assignments;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.open.spring.mvc.groups.Groups;
import com.open.spring.mvc.groups.GroupsJpaRepository;
import com.open.spring.mvc.groups.Submitter;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;
import com.open.spring.mvc.synergy.SynergyGrade;
import com.open.spring.mvc.synergy.SynergyGradeJpaRepository;

import jakarta.transaction.Transactional;
import javassist.tools.web.BadHttpRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * REST API Controller for managing assignment submissions.
 * Provides endpoints for CRUD operations on assignment submissions.
 */
@RestController
@RequestMapping("/api/submissions")
public class AssignmentSubmissionAPIController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AssignmentSubmissionJPA submissionRepo;

    @Autowired
    private AssignmentJpaRepository assignmentRepo;

    @Autowired
    private PersonJpaRepository personRepo;

    @Autowired
    private GroupsJpaRepository groupRepo;

    @Autowired
    private SynergyGradeJpaRepository gradesRepo;
    
    /**
     * A DTO class for returning only necessary assignment submission details.
     */
    @Getter
    @Setter
    public static class AssignmentReturnDto {
        public Long id;
        public String name;
        public String type;
        public String description;
        public Double points;
        public String dueDate;
        public String timestamp;

        public AssignmentReturnDto(Assignment assignment) {
            this.id = assignment.getId();
            this.name = assignment.getName();
            this.type = assignment.getType();
            this.description = assignment.getDescription();
            this.points = assignment.getPoints();
            this.dueDate = assignment.getDueDate();
            this.timestamp = assignment.getTimestamp();
        }
    }

    /**
     * Get all submissions for a specific student.
     * 
     * @param studentId the ID of the student whose submissions are to be fetched
     * @return a ResponseEntity containing a list of submissions for the given student ID
     */
    @Transactional
    @GetMapping("/getSubmissions/{studentId}")
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
     * A DTO class with the format for the JSON when submitting assignments.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SubmitAssignmentDto {
        public Long submitterId;
        public Boolean isGroup;
        public String content;
        public String comment;
        public Boolean isLate;
    }

    /**
     * A POST endpoint to submit an assignment.
     * @param assignmentId The ID of the assignment being submitted.
     * @param studentId The ID of the student submitting the assignment.
     * @param content The content of the student's submission.
     * @return The saved submission, if it successfully submitted.
     */
    @PostMapping("/submit/{assignmentId}")
    public ResponseEntity<?> submitAssignment(
        @PathVariable Long assignmentId,
        @RequestBody SubmitAssignmentDto submissionInfo
    ) {
        Assignment assignment = assignmentRepo.findById(assignmentId).orElse(null);
        
        // TODO: A better way to do this would be to have this be part of some sort of SubmitterService
        Submitter submitter;
        if (submissionInfo.isGroup) {
            submitter = groupRepo.findById(submissionInfo.submitterId).orElse(null);
        } else {
            submitter = personRepo.findById(submissionInfo.submitterId).orElse(null);
        }
        
        if (submitter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Submitter not found");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
        
        if (assignment != null) {
            AssignmentSubmission submission = new AssignmentSubmission(assignment, submitter, submissionInfo.content, submissionInfo.comment, submissionInfo.isLate);
            AssignmentSubmission savedSubmission = submissionRepo.save(submission);
            return new ResponseEntity<>(new AssignmentSubmissionReturnDto(savedSubmission), HttpStatus.CREATED);
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "Assignment not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @Getter
    @Setter
    public static class SubmissionRequestDto {
        public Long assignmentId;
        public Long submitterId;
        public Boolean isGroupSubmission;
        public String content;
        public String comment;
        public Boolean isLate;
    }

    /**
     * Submit an assignment for a student.
     * 
     * @param assignmentId the ID of the assignment being submitted
     * @param studentId    the ID of the student submitting the assignment
     * @param content      the content of the submission
     * @param comment      any comments related to the submission
     * @return a ResponseEntity containing the created submission or an error if the assignment is not found
     */
    @PostMapping("/{assignmentId}")
    public ResponseEntity<?> submitAssignment(
            @RequestBody SubmissionRequestDto requestData
    ) {
        Assignment assignment = assignmentRepo.findById(requestData.assignmentId).orElse(null);

        if (assignment != null) {
            Submitter submitter;
            if (requestData.isGroupSubmission) {
                submitter = groupRepo.findById(requestData.submitterId).orElse(null);
            } else {
                submitter = personRepo.findById(requestData.submitterId).orElse(null);
            }

            AssignmentSubmission submission = new AssignmentSubmission(assignment, submitter, requestData.content, requestData.comment,requestData.isLate);
            AssignmentSubmission savedSubmission = submissionRepo.save(submission);
            return new ResponseEntity<>(new AssignmentSubmissionReturnDto(savedSubmission), HttpStatus.CREATED);
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "Assignment not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Grade an existing assignment submission.
     * 
     * @param submissionId the ID of the submission to be graded
     * @param grade        the grade to be assigned to the submission
     * @param feedback     optional feedback for the submission
     * @return a ResponseEntity indicating success or an error if the submission is not found
     */
    @PostMapping("/grade/{submissionId}")
    @Transactional
    public ResponseEntity<?> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestParam Double grade,
            @RequestParam(required = false) String feedback
    ) {
        AssignmentSubmission submission = submissionRepo.findById(submissionId).orElse(null);
        if (submission == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Submission not found");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);    
        }

        // we have a correct submission
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submissionRepo.save(submission);

        for (Person student : submission.getSubmitter().getMembers()) {
            SynergyGrade assignedGrade = gradesRepo.findByAssignmentAndStudent(submission.getAssignment(), student);
            if (assignedGrade != null) {
                // the assignment has a previously assigned grade, so we are just updating it
                assignedGrade.setGrade(grade);
                gradesRepo.save(assignedGrade);
            }
            else {
                // assignment is not graded, we must create a new grade
                SynergyGrade newGrade = new SynergyGrade(grade, submission.getAssignment(), student);
                gradesRepo.save(newGrade);
            }
        }

        return new ResponseEntity<>("Grade updated successfully", HttpStatus.OK);
    }

    /**
     * Get all submissions for a specific assignment.
     * 
     * @param assignmentId the ID of the assignment whose submissions are to be fetched
     * @return a ResponseEntity containing a list of submissions or an error if the assignment is not found
     */
    @Transactional
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<?> getSubmissionsByAssignment(@PathVariable Long assignmentId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        String uid = userDetails.getUsername();
        Person user = personRepo.findByUid(uid);
        if (user == null) {
            logger.error("User not found with email: {}", uid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with uid: " + uid);
        }

        Assignment assignment = assignmentRepo.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return new ResponseEntity<>(
                Collections.singletonMap("error", "Assignment not found"), 
                HttpStatus.NOT_FOUND
            );
        }

        List<AssignmentSubmission> submissions = submissionRepo.findByAssignmentId(assignmentId);
        List<AssignmentSubmissionReturnDto> submissionsReturn;

        if (!(user.hasRoleWithName("ROLE_TEACHER") || user.hasRoleWithName("ROLE_ADMIN"))) {
            // if they aren't a teacher or admin, only let them see submissions they are assigned to grade
            submissionsReturn = submissions.stream()
                .filter(submission -> submission.getAssignedGraders().contains(user))
                .map(AssignmentSubmissionReturnDto::new)
                .collect(Collectors.toList());
        } else {
            submissionsReturn = submissions.stream()
                .map(AssignmentSubmissionReturnDto::new)
                .collect(Collectors.toList());
        }
    
        return new ResponseEntity<>(submissionsReturn, HttpStatus.OK);
    }

    /**
     * Assign persons as graders to a specific submission.
     * @param id the ID of the submission to which graders are being assigned
     * @param personIds a list of person IDs to be assigned as graders
     * @return a ResponseEntity indicating success or failure
     */
    @PostMapping("/{id}/assigned-graders")
    public ResponseEntity<?> assignGradersToSubmission(@PathVariable Long id, @RequestBody List<Long> personIds) {
        Optional<AssignmentSubmission> submissionOptional = submissionRepo.findById(id);
        if (!submissionOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submission not found");
        }

        AssignmentSubmission submission = submissionOptional.get();
        List<Person> persons = personRepo.findAllById(personIds);

        submission.setAssignedGraders(persons);

        submissionRepo.save(submission);
        return ResponseEntity.ok("Persons assigned successfully");
    }

    /**
     * Get the IDs of persons assigned as graders for a specific submission.
     * 
     * @param id the ID of the submission whose assigned graders are to be fetched
     * @return a ResponseEntity containing a list of assigned grader IDs or an error if the submission is not found
     */
    @GetMapping("/{id}/assigned-graders")
    public ResponseEntity<?> getAssignedGraders(@PathVariable Long id) {
        Optional<AssignmentSubmission> submissionOptional = submissionRepo.findById(id);
        if (!submissionOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submission not found");
        }

        AssignmentSubmission submission = submissionOptional.get();
        List<Person> assignedGraders = submission.getAssignedGraders();
        
        // Return just the IDs of assigned persons
        List<Long> assignedGraderIds = assignedGraders.stream()
            .map(Person::getId)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(assignedGraderIds);
    }

    /**
     * Extract all submissions for a specific assignment.
     * 
     * @param assignmentId the ID of the assignment whose submissions are to be extracted
     * @return a ResponseEntity containing a list of all submissions for the assignment
     */
    @GetMapping("/extract/{assignmentId}")
    @Transactional
    public ResponseEntity<?> extractSubmissionsByAssignment(@PathVariable Long assignmentId) {
        Assignment assignment = assignmentRepo.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return new ResponseEntity<>(
                Collections.singletonMap("error", "Assignment not found"), 
                HttpStatus.NOT_FOUND
            );
        }

        List<AssignmentSubmission> submissions = submissionRepo.findByAssignmentId(assignmentId);
        List<AssignmentSubmissionReturnDto> submissionDtos = submissions.stream()
            .map(AssignmentSubmissionReturnDto::new)
            .collect(Collectors.toList());

        return new ResponseEntity<>(submissionDtos, HttpStatus.OK);
    }
}