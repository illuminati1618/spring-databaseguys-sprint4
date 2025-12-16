package com.open.spring.mvc.assignments;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

@Controller
@RequestMapping("/mvc/assignments")
public class AssignmentViewController {

    @Autowired
    private AssignmentJpaRepository assignmentRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    @Autowired
    private AssignmentSubmissionJPA submissionRepo;

    @GetMapping("/tracker")
    public String assignmentTracker(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String uid = userDetails.getUsername();
        Person user = personRepository.findByUid(uid);
        
        if (user == null || (!user.hasRoleWithName("ROLE_TEACHER") && !user.hasRoleWithName("ROLE_ADMIN"))) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a teacher or admin to access the assignment tracker"
            );
        }

        return "assignments/assignment_tracker";
    }

    @GetMapping
    public String listAssignments(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String uid = userDetails.getUsername();
        Person user = personRepository.findByUid(uid);
        
        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a logged-in user to view assignments"
            );
        }

        List<Assignment> assignments = assignmentRepository.findAll();
        model.addAttribute("assignments", assignments);

        // If user is a student, show student view
        if (user.hasRoleWithName("ROLE_STUDENT")) {
            return "assignments/student_assignments";
        } 
        // If user is a teacher or admin, show all assignments
        else if (user.hasRoleWithName("ROLE_TEACHER") || user.hasRoleWithName("ROLE_ADMIN")) {
            return "assignments/teacher_assignments";
        }

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must be a student, teacher, or admin to view assignments"
        );
    }

    @GetMapping("/{id}")
    public String viewAssignmentDetails(
            @PathVariable Long id, 
            Model model, 
            @AuthenticationPrincipal UserDetails userDetails) {
        String uid = userDetails.getUsername();
        Person user = personRepository.findByUid(uid);
        
        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a logged-in user to view assignment details"
            );
        }

        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Assignment not found"
            ));
        
        model.addAttribute("assignment", assignment);

        // If user is a student, show student view
        if (user.hasRoleWithName("ROLE_STUDENT")) {
            return "assignments/student_assignment_details";
        } 
        // If user is a teacher or admin, show detailed view
        else if (user.hasRoleWithName("ROLE_TEACHER") || user.hasRoleWithName("ROLE_ADMIN")) {
            return "assignments/teacher_assignment_details";
        }

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must be a student, teacher, or admin to view assignment details"
        );
    }

    @GetMapping("/{id}/submissions")
    public String viewAssignmentSubmissions(
            @PathVariable Long id, 
            Model model, 
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String uid = userDetails.getUsername();
        Person user = personRepository.findByUid(uid);
        
        if (user == null || (!user.hasRoleWithName("ROLE_TEACHER") && !user.hasRoleWithName("ROLE_ADMIN"))) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a teacher or admin to view assignment submissions"
            );
        }

        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Assignment not found"
            ));

        List<AssignmentSubmission> submissions = submissionRepo.findByAssignmentId(id);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        return "assignments/assignment_submissions";
    }

    @GetMapping("/queue-management")
    public String queueManagement(){
        return "assignments/queue_management";
    }

    @GetMapping("/read")
    public String read(Model model) {
        List<Assignment> assignments = assignmentRepository.findAll();
        model.addAttribute("list", assignments);
        return "assignments/read";
    }
}