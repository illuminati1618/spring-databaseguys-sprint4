package com.open.spring.mvc.bathroom;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;

/**
 * This class provides RESTful API endpoints for managing BathroomQueue
 * entities.
 * It includes endpoints for creating, retrieving, updating, and managing
 * bathroom
 * queue operations for classroom management.
 */
@RestController
@RequestMapping("/api/bathroom") // Updated mapping to match frontend
@CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com/" })
public class BathroomQueueApiController {

    /**
     * Repository for accessing BathroomQueue entities in the database.
     */
    @Autowired
    private BathroomQueueJPARepository repository;

    /**
     * DTO (Data Transfer Object) to support request operations for queue
     * management.
     * Contains necessary information for student queue operations.
     */
    @Getter
    public static class QueueDto {
        private String teacherEmail; // Teacher's email associated with the queue
        private String studentName; // Name of the student to be added/removed/approved
        private String uri; // URI for constructing approval links
    }

    /**
     * DTO (Data Transfer Object) to support POST request for addQueue method.
     * Represents the data required to create a new bathroom queue.
     */
    @Getter
    public static class QueueAddReq {
        private String teacherEmail; // Teacher's email to associate with the new queue
        private String peopleQueue; // Initial student(s) to add to the queue
    }

    /**
     * Create a new BathroomQueue entity for a teacher.
     * 
     * @param request The QueueAddReq object containing teacher email and initial
     *                queue data
     * @return A ResponseEntity containing a success message if the queue is
     *         created,
     *         or a CONFLICT status if queue already exists, or
     *         INTERNAL_SERVER_ERROR if creation fails
     */
    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @PostMapping("/addQueue")
    public ResponseEntity<String> addQueue(@RequestBody QueueAddReq request) {
        System.out.println(request);

        try {
            // Check if a queue already exists for the given teacher email
            Optional<BathroomQueue> existingQueue = repository.findByTeacherEmail(request.getTeacherEmail());
            if (existingQueue.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Queue already exists for this teacher.");
            }

            // Create and save a new queue if it doesn't exist
            BathroomQueue newQueue = new BathroomQueue(request.getTeacherEmail(), request.getPeopleQueue());
            repository.save(newQueue);
            return ResponseEntity.ok("Queue added successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add queue: " + e.getMessage());
        }
    }

    /**
     * Add a student to an existing bathroom queue or create a new queue if none
     * exists.
     * 
     * @param queueDto The QueueDto object containing teacher email and student name
     * @return A ResponseEntity containing a success message with student and
     *         teacher information,
     *         or a CREATED status if operation is successful
     */
    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @PostMapping("/add")
    public ResponseEntity<Object> addToQueue(@RequestBody QueueDto queueDto) {
        // Check if a queue already exists for the given teacher
        Optional<BathroomQueue> existingQueue = repository.findByTeacherEmail(queueDto.getTeacherEmail());

        if (existingQueue.isPresent()) {
            BathroomQueue queue = existingQueue.get();
            if (queue.containsStudent(queueDto.getStudentName())) {
                // TOGGLE: Student is already in queue, so remove them (checking back in)
                queue.removeStudent(queueDto.getStudentName());
                repository.save(queue);
                return new ResponseEntity<>(Map.of(
                        "action", "removed",
                        "message", queueDto.getStudentName() + " has checked back in."), HttpStatus.OK);
            } else {
                // TOGGLE: Student is not in queue, so add them
                queue.addStudent(queueDto.getStudentName());
                repository.save(queue);
                return new ResponseEntity<>(Map.of(
                        "action", "added",
                        "message", queueDto.getStudentName() + " was added to the queue."), HttpStatus.CREATED);
            }
        } else {
            // Create a new queue for the teacher and add the student
            BathroomQueue newQueue = new BathroomQueue(queueDto.getTeacherEmail(), queueDto.getStudentName());
            repository.save(newQueue);
            return new ResponseEntity<>(Map.of(
                    "action", "added",
                    "message",
                    queueDto.getStudentName() + " was added to a new queue for " + queueDto.getTeacherEmail()),
                    HttpStatus.CREATED);
        }
    }

    /**
     * Remove a specific student from a teacher's bathroom queue.
     * 
     * @param queueDto The QueueDto object containing teacher email and student name
     *                 to remove
     * @return A ResponseEntity containing a success message if student is removed,
     *         or a NOT_FOUND status if queue or student is not found
     */
    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @DeleteMapping("/remove")
    public ResponseEntity<Object> removeFromQueue(@RequestBody QueueDto queueDto) {
        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(queueDto.getTeacherEmail());

        if (queueEntry.isPresent()) {
            BathroomQueue bathroomQueue = queueEntry.get();

            try {
                // Remove the student from the queue
                bathroomQueue.removeStudent(queueDto.getStudentName());
                repository.save(bathroomQueue);
                return new ResponseEntity<>("Removed " + queueDto.getStudentName(), HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            }
        }

        return new ResponseEntity<>("Queue for " + queueDto.getTeacherEmail() + " not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Remove the first student from a teacher's bathroom queue.
     * 
     * @param teacher The teacher's email whose queue's front student should be
     *                removed
     * @return void - This method does not return a ResponseEntity (consider adding
     *         one for better API design)
     */
    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @PostMapping("/removefront/{teacher}")
    public void removeFront(@PathVariable String teacher) {
        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(teacher);
        BathroomQueue bathroomQueue = queueEntry.get();
        String firstStudent = bathroomQueue.getFrontStudent();
        bathroomQueue.removeStudent(firstStudent);
        repository.save(bathroomQueue);
    }

    /**
     * Approve the first student in a teacher's bathroom queue.
     * Only the student at the front of the queue can be approved.
     * 
     * @param queueDto The QueueDto object containing teacher email and student name
     *                 to approve
     * @return A ResponseEntity containing a success message if student is approved,
     *         BAD_REQUEST if student is not at front of queue, or NOT_FOUND if
     *         queue doesn't exist
     */
    @CrossOrigin(origins = { "*" })
    @PostMapping("/approve")
    public ResponseEntity<Object> approveStudent(@RequestBody QueueDto queueDto) {
        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(queueDto.getTeacherEmail());

        if (queueEntry.isPresent()) {
            BathroomQueue bathroomQueue = queueEntry.get();
            try {
                bathroomQueue.approveStudent();
                repository.save(bathroomQueue);
                return new ResponseEntity<>("Approved student for " + queueDto.getTeacherEmail(), HttpStatus.OK);
            } catch (IllegalStateException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>("Queue for " + queueDto.getTeacherEmail() + " not found", HttpStatus.NOT_FOUND);
    }

    @CrossOrigin(origins = { "*" })
    @PostMapping("/updateMaxOccupancy")
    public ResponseEntity<Object> updateMaxOccupancy(@RequestBody Map<String, Object> payload) {
        String teacherEmail = (String) payload.get("teacherEmail");
        Integer maxOccupancy = (Integer) payload.get("maxOccupancy");

        if (teacherEmail == null || maxOccupancy == null) {
            return new ResponseEntity<>("teacherEmail and maxOccupancy are required", HttpStatus.BAD_REQUEST);
        }

        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(teacherEmail);
        if (queueEntry.isPresent()) {
            BathroomQueue queue = queueEntry.get();
            queue.setMaxOccupancy(maxOccupancy);
            repository.save(queue);
            return new ResponseEntity<>("Updated maxOccupancy to " + maxOccupancy, HttpStatus.OK);
        }

        return new ResponseEntity<>("Queue for " + teacherEmail + " not found", HttpStatus.NOT_FOUND);
    }

    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @PostMapping("/removeFront")
    public ResponseEntity<Object> removeFrontStudent(@RequestBody QueueDto queueDto) {
        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(queueDto.getTeacherEmail());

        if (queueEntry.isPresent()) {
            BathroomQueue bathroomQueue = queueEntry.get();
            String queue = bathroomQueue.getPeopleQueue();

            if (queue != null && !queue.isEmpty()) {
                String[] students = queue.split(",");
                if (students.length > 1) {
                    // Remove first student and rebuild queue
                    String newQueue = String.join(",", Arrays.copyOfRange(students, 1, students.length));
                    bathroomQueue.setPeopleQueue(newQueue);
                } else {
                    // Only one student in queue
                    bathroomQueue.setPeopleQueue("");
                }

                repository.save(bathroomQueue);
                return ResponseEntity.ok("Removed front student from queue");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Queue is already empty");
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Queue not found");
    }

    /**
     * Approve a student via a direct link with query parameters.
     * This endpoint allows teachers to approve students through email links or
     * direct URLs.
     * 
     * @param teacherEmail The teacher's email associated with the queue
     * @param studentName  The name of the student to approve
     * @return A ResponseEntity containing a success message if student is approved,
     *         BAD_REQUEST if student is not at front of queue, or NOT_FOUND if
     *         queue doesn't exist
     */
    @GetMapping("/approveLink")
    public ResponseEntity<Object> approveStudentViaLink(@RequestParam String teacherEmail,
            @RequestParam String studentName) {
        Optional<BathroomQueue> queueEntry = repository.findByTeacherEmail(teacherEmail);
        if (queueEntry.isPresent()) {
            BathroomQueue bathroomQueue = queueEntry.get();
            String frontStudent = bathroomQueue.getFrontStudent();
            if (frontStudent != null && frontStudent.equals(studentName)) {
                // Approve the student
                repository.save(bathroomQueue);
                return new ResponseEntity<>("Approved " + studentName, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Student is not at the front of the queue", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>("Queue for " + teacherEmail + " not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Retrieves all BathroomQueue entities in the database.
     * 
     * @return A ResponseEntity containing a list of all BathroomQueue entities
     */
    @GetMapping("/all")
    public ResponseEntity<List<BathroomQueue>> getAllQueues() {
        return new ResponseEntity<>(repository.findAll(), HttpStatus.OK);
    }

    /**
     * Retrieves all active bathroom queues.
     * Currently returns all queues - consider implementing filtering for truly
     * "active" queues.
     * 
     * @return A ResponseEntity containing a list of all BathroomQueue entities
     */
    @CrossOrigin(origins = { "http://localhost:8585", "https://pages.opencodingsociety.com" })
    @GetMapping("/getActive")
    public ResponseEntity<Object> getActiveQueues() {
        return new ResponseEntity<>(repository.findAll(), HttpStatus.OK);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearTable(@RequestParam(required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Unauthorized — Admin access required"));
        }

        repository.deleteAllRowsInBulk();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All bathroom queue records have been cleared"));
    }

    /**
     * Retrieves a specific teacher's bathroom queue.
     * 
     * @param teacherEmail The teacher's email associated with the queue
     * @return A ResponseEntity containing the BathroomQueue entity if found
     */
    @GetMapping("/queue/{teacherEmail}")
    public ResponseEntity<BathroomQueue> getQueueByTeacher(@PathVariable String teacherEmail) {
        return repository.findByTeacherEmail(teacherEmail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    BathroomQueue emptyQueue = new BathroomQueue(teacherEmail, "");
                    emptyQueue.setMaxOccupancy(1);
                    return ResponseEntity.ok(emptyQueue);
                });
    }
}