package com.open.spring.mvc.person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.open.spring.mvc.bathroom.TinkleJPARepository;

import lombok.Getter;
import lombok.Setter;


/**
 * This class provides RESTful API endpoints for managing Person entities.
 * It includes endpoints for creating, retrieving, updating, and deleting Person
 * entities.
 */
@RestController
@RequestMapping("/api")
public class PersonApiController {
    private static final Logger logger = LoggerFactory.getLogger(PersonApiController.class);

    /*
     * #### RESTful API REFERENCE ####
     * Resource: https://spring.io/guides/gs/rest-service/
     */

    /**
     * Repository for accessing Person entities in the database.
     */
    @Autowired
    private PersonJpaRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Service for managing Person entities.
     */
    @Autowired
    private PersonDetailsService personDetailsService;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private TinkleJPARepository tinkleRepository;

    /**
     * Retrieves a Person entity by current user of JWT token.
     * 
     * @return A ResponseEntity containing the Person entity if found, or a
     *         NOT_FOUND status if not found.
     */
    @GetMapping("/person/get")
    public ResponseEntity<Person> getPerson(@AuthenticationPrincipal UserDetails userDetails) {
        // Check if the user is not logged in
        if (userDetails == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String email = userDetails.getUsername(); // Email is mapped/unmapped to username for Spring Security

        // Find a person by username
        Person person = repository.findByUid(email);

        // Return the person if found
        if (person != null) {
            return new ResponseEntity<>(person, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }



    /**
     * Retrieves a Person entity by its UID.
     *
     * @param uid The UID of the Person entity to retrieve.
     * @return A ResponseEntity containing the Person entity if found, or a
     *         NOT_FOUND status if not found.
     */
    @GetMapping("/person/uid/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public ResponseEntity<Person> getPersonByUid(@PathVariable String uid) {
        Person person = repository.findByUid(uid);
        if (person != null) {
            return new ResponseEntity<>(person, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Retrieves all Person entities that have face data registered.
     * Returns a list of maps containing 'uid' and 'faceData'.
     * 
     * @return A ResponseEntity containing a list of maps with uid and faceData.
     */
    @GetMapping("/person/faces")
    // Public for bathroom-pass face matcher (self-service). Security is via token/cookie in other workflows.
    public ResponseEntity<List<Map<String, String>>> getPersonFaces() {
        List<Person> people = repository.findByFaceDataIsNotNull();
        List<Map<String, String>> faces = new ArrayList<>();
        for (Person person : people) {
            Map<String, String> face = new HashMap<>();
            face.put("uid", person.getUid());
            face.put("faceData", person.getFaceData());
            faces.add(face);
        }
        return new ResponseEntity<>(faces, HttpStatus.OK);
    }

    /**
     * Identify a person from the provided camera image using face data stored in Spring.
     *
     * This implementation now runs DeepFace through a local Python subprocess rather than proxying to Flask.
     *
     * @param body map containing "image" (base64) and optional "threshold"
     * @return map containing match result or error
     */
    @PostMapping("/person/identify")
    public ResponseEntity<Map<String, Object>> identifyPerson(@RequestBody Map<String, Object> body) {
        String image = (String) body.get("image");
        Double thresholdValue = 0.40; // Default threshold

        if (body.containsKey("threshold")) {
            Object thresholdObj = body.get("threshold");
            if (thresholdObj instanceof Number) {
                thresholdValue = ((Number) thresholdObj).doubleValue();
            } else if (thresholdObj instanceof String) {
                try {
                    thresholdValue = Double.parseDouble((String) thresholdObj);
                } catch (NumberFormatException e) {
                    // Fall back to default
                }
            }
        }

        if (image == null || image.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("match", false);
            error.put("message", "No image provided");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        logger.info("IDENTIFY request received. Image length: {}", image.length());
        Map<String, Object> result = faceRecognitionService.identify(image, thresholdValue);
        logger.info("IDENTIFY result: {}", result.get("match"));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Update a Person entity by its ID (using current authenticated user).
     *
     * 
     * @param authentication Current authentication context.
     * @param personDto The updated PersonDto object.
     * @return A ResponseEntity containing the updated Person entity if found, or a
     *         NOT_FOUND status if not found.
     */
    @PostMapping(value = "/person/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> updatePerson(Authentication authentication, @RequestBody final PersonDto personDto) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // Get the email of the current user from the authentication context
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // Assuming email is used as the username in Spring Security

        // Find the person by email
        Optional<Person> optionalPerson = Optional.ofNullable(repository.findByUid(email));
        if (optionalPerson.isPresent()) {
            Person existingPerson = optionalPerson.get();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

            boolean emailChanged = personDto.getEmail() != null
                    && !personDto.getEmail().equals(existingPerson.getEmail());
            boolean passwordChanged = personDto.getPassword() != null && !personDto.getPassword().isBlank();
            boolean sidChanged = personDto.getSid() != null && !personDto.getSid().equals(existingPerson.getSid());
            boolean nameChanged = personDto.getName() != null && !personDto.getName().equals(existingPerson.getName());
            boolean pfpChanged = personDto.getPfp() != null && !personDto.getPfp().equals(existingPerson.getPfp());
            boolean kasmChanged = personDto.getKasmServerNeeded() != null
                    && !personDto.getKasmServerNeeded().equals(existingPerson.getKasmServerNeeded());
            boolean uidChangeRequested = personDto.getUid() != null
                    && !personDto.getUid().equals(existingPerson.getUid());
            
            // Log faceData receipt for debugging
            if (personDto.getFaceData() != null) {
                logger.info("UPDATE received faceData for user: {}. Length: {}", email, personDto.getFaceData().length());
            }

            boolean faceDataChanged = personDto.getFaceData() != null
                    && !personDto.getFaceData().equals(existingPerson.getFaceData());

            if (!isAdmin && uidChangeRequested) {
                logger.warn("AUDIT profile_update_blocked actor={} reason=uid_change_not_allowed",
                        existingPerson.getUid());
                JSONObject responseObject = new JSONObject();
                responseObject.put("error", "UID cannot be changed through this endpoint");
                return new ResponseEntity<>(responseObject.toString(), HttpStatus.FORBIDDEN);
            }

            StringBuilder changedFields = new StringBuilder();
            if (emailChanged) {
                // Check if email is already taken by another person
                Person personWithEmail = repository.findByEmail(personDto.getEmail());
                if (personWithEmail != null && !personWithEmail.getId().equals(existingPerson.getId())) {
                    JSONObject responseObject = new JSONObject();
                    responseObject.put("error", "A person with email '" + personDto.getEmail() + "' already exists");
                    return new ResponseEntity<>(responseObject.toString(), HttpStatus.CONFLICT);
                }
                existingPerson.setEmail(personDto.getEmail());
                changedFields.append("email,");
            }
            if (passwordChanged) {
                existingPerson.setPassword(passwordEncoder.encode(personDto.getPassword()));
                changedFields.append("password,");
            }
            if (isAdmin && uidChangeRequested) {
                // Check if uid is already taken by another person
                Person personWithUid = repository.findByUid(personDto.getUid());
                if (personWithUid != null && !personWithUid.getId().equals(existingPerson.getId())) {
                    JSONObject responseObject = new JSONObject();
                    responseObject.put("error", "A person with uid '" + personDto.getUid() + "' already exists");
                    return new ResponseEntity<>(responseObject.toString(), HttpStatus.CONFLICT);
                }
                existingPerson.setUid(personDto.getUid());
                changedFields.append("uid,");
            }
            if (sidChanged) {
                existingPerson.setSid(personDto.getSid());
                changedFields.append("sid,");
            }

            if (nameChanged) {
                existingPerson.setName(personDto.getName());
                changedFields.append("name,");
            }
            if (pfpChanged) {
                existingPerson.setPfp(personDto.getPfp());
                changedFields.append("pfp,");
            }
            if (kasmChanged) {
                existingPerson.setKasmServerNeeded(personDto.getKasmServerNeeded());
                changedFields.append("kasmServerNeeded,");
            }
            if (faceDataChanged) {
                existingPerson.setFaceData(personDto.getFaceData());
                changedFields.append("faceData,");
                logger.info("Persisting faceData for user: {}. Length: {}", email, existingPerson.getFaceData().length());
            }
            // Save the updated person back to the repository
            Person updatedPerson = repository.save(existingPerson);

            String changed = changedFields.length() == 0
                    ? "none"
                    : changedFields.substring(0, changedFields.length() - 1);
            logger.info("AUDIT profile_update actor={} target={} fields={} admin={}", email, updatedPerson.getUid(),
                    changed, isAdmin);

            // Return the updated person entity
            return new ResponseEntity<>(updatedPerson, HttpStatus.OK);
        }

        // Return NOT_FOUND if person not found
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Retrieves a Person entity by its ID.
     *
     * @param id The ID of the Person entity to retrieve.
     * @return A ResponseEntity containing the Person entity if found, or a
     *         NOT_FOUND status if not found.
     */
    @GetMapping("/person/{id}")
    public ResponseEntity<Person> getPerson(@PathVariable long id) {
        Optional<Person> optional = repository.findById(id);
        if (optional.isPresent()) { // Good ID
            Person person = optional.get(); // value from findByID
            return new ResponseEntity<>(person, HttpStatus.OK); // OK HTTP response: status code, headers, and body
        }
        // Bad ID
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Retrieves a Person entity by its UID.
     *
     * @param uid The UID of the Person entity to retrieve.
     * @return A ResponseEntity containing the Person entity if found, or a
     *         NOT_FOUND status if not found.
     */
    @GetMapping("/person/uid/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public ResponseEntity<Person> getPersonByUid(@PathVariable String uid) {
        Person person = repository.findByUid(uid);
        if (person != null) {
            return new ResponseEntity<>(person, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Delete a Person entity by its ID.
     *
     * @param id The ID of the Person entity to delete.
     * @return A ResponseEntity containing the Person entity if deleted, or a
     *         NOT_FOUND status if not found.
     */
    @DeleteMapping("/person/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Person> deletePerson(@PathVariable long id) {
        Optional<Person> optional = repository.findById(id);
        if (optional.isPresent()) { // Good ID
            Person person = optional.get(); // value from findByID
            repository.deleteById(id); // value from findByID
            return new ResponseEntity<>(person, HttpStatus.OK); // OK HTTP response: status code, headers, and body
        }
        // Bad ID
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /*
     * DTO (Data Transfer Object) to support POST request for postPerson method
     * .. represents the data in the request body
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class PersonDto {
        private String email;
        private String uid;
        private String sid;
        private String password;
        private String currentPassword;
        private String name;
        private String pfp;
        private Boolean kasmServerNeeded;
        private String faceData;
    }

    /**
     * Create a new Person entity.
     * 
     * @param personDto
     * @return A ResponseEntity containing a success message if the Person entity is
     *         created, or a BAD_REQUEST status if not created.
     */
    @PostMapping("/person/create")
    public ResponseEntity<Object> postPerson(@RequestBody PersonDto personDto) {

        // Check if a person with this uid already exists
        if (personDto.getUid() != null && repository.existsByUid(personDto.getUid())) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject responseObject = new JSONObject();
            responseObject.put("error", "A person with uid '" + personDto.getUid() + "' already exists");
            return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.CONFLICT);
        }

        // Check if a person with this email already exists
        if (personDto.getEmail() != null && repository.existsByEmail(personDto.getEmail())) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject responseObject = new JSONObject();
            responseObject.put("error", "A person with email '" + personDto.getEmail() + "' already exists");
            return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.CONFLICT);
        }

        if (personDto.getSid() != null && !personDto.getSid().isBlank() && tinkleRepository.findBySid(personDto.getSid()).isPresent()) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject responseObject = new JSONObject();
            responseObject.put("error", "A person with sid '" + personDto.getSid() + "' already exists");
            return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.CONFLICT);
        }

        // Use canonical Spring Security role naming (ROLE_*) for new accounts.
        PersonRole defaultRole = personDetailsService.findRole("ROLE_USER");
        if (defaultRole == null) {
            // Backward compatibility for deployments that still have legacy role names.
            defaultRole = personDetailsService.findRole("USER");
        }
        if (defaultRole == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject responseObject = new JSONObject();
            responseObject.put("error", "Default role ROLE_USER is not configured");
            return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // A person object WITHOUT ID will create a new record in the database
        Person person = new Person(personDto.getEmail(), personDto.getUid(), personDto.getPassword(),
                personDto.getSid(), personDto.getName(), "/images/default.png", true, defaultRole);

        try {
            personDetailsService.save(person);
        } catch (DataIntegrityViolationException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject responseObject = new JSONObject();
            responseObject.put("error", "Unable to create user due to duplicate constrained fields (likely uid/email/sid)");
            return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.CONFLICT);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject responseObject = new JSONObject();
        responseObject.put("response", personDto.getEmail() + " is created successfully");

        return new ResponseEntity<>(responseObject.toString(), responseHeaders, HttpStatus.OK);
    }

    @Autowired
    private PersonService personService;

    @PostMapping("/grade-frqs")
    public ResponseEntity<String> gradeFrqs() {
        personService.gradeAllPending();
        return ResponseEntity.ok("Grading completed. Check ./volumes/grades.csv for results.");
    }

    /**
     * Retrieves all the Person entities in the database, people
     * 
     * @return A ResponseEntity containing a list for Person entities
     * @throws JsonProcessingException
     */
    @GetMapping("/people")
    public ResponseEntity<?> getPeople() throws JsonProcessingException {
        // Fetch the data from the repository into a variable
        List<Person> people = repository.findAllByOrderByNameAsc();

        // Return the variable in the ResponseEntity
        return new ResponseEntity<>(people, HttpStatus.OK);
    }

    /**
     * Retrieves a single page of the Person entities in the database, people
     * 
     * @param personId The starting index of the page to retrieve.
     * @param pageSize The number of Person entities to include in the page.
     * @return A ResponseEntity containing a paginated list for Person entities
     * @throws JsonProcessingException
     */
    @GetMapping("/people/page/{personId}")
    public ResponseEntity<?> getPeoplePage(@PathVariable int personId, @RequestParam int pageSize)
            throws JsonProcessingException {
        List<Person> allPeople = repository.findAllByOrderByNameAsc();
        int total = allPeople.size();

        int start = Math.max(0, personId);
        int end = Math.min(start + pageSize, total);

        List<Person> pageData = allPeople.subList(start, end);

        // Build paging URLs
        String baseUrl = "/api/people/page/";
        String previous = start > 0 ? baseUrl + Math.max(0, start - pageSize) + "?pageSize=" + pageSize : null;
        String next = end < total ? baseUrl + end + "?pageSize=" + pageSize : null;

        Map<String, Object> paging = new HashMap<>();
        paging.put("previous", previous);
        paging.put("next", next);

        Map<String, Object> response = new HashMap<>();
        response.put("data", pageData);
        response.put("paging", paging);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Bulk create Person entities from a list of PersonDto objects.
     * 
     * @param personDtos A list of PersonDto objects to be created.
     * @return A ResponseEntity containing the result of the bulk creation.
     */
    @PostMapping("/people/bulk/create")
    public ResponseEntity<Object> bulkCreatePersons(@RequestBody List<PersonDto> personDtos) {
        List<String> createdPersons = new ArrayList<>();
        List<String> duplicatePersons = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (PersonDto personDto : personDtos) {
            try {
                // Call the existing postPerson method
                ResponseEntity<Object> response = postPerson(personDto);

                // Check if the response is successful
                if (response.getStatusCode() == HttpStatus.OK) {
                    createdPersons.add(personDto.getEmail());
                } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                    // Handle duplicate uid or email
                    duplicatePersons.add(personDto.getUid() != null ? personDto.getUid() : personDto.getEmail());
                } else {
                    errors.add("Failed to create person with email: " + personDto.getEmail());
                }
            } catch (Exception e) {
                // Check if the exception is caused by a unique constraint violation
                if (e.getCause() != null && e.getCause().getMessage().contains("constraint [email]")) {
                    duplicatePersons.add(personDto.getEmail());
                } else if (e.getCause() != null && e.getCause().getMessage().contains("constraint [uid]")) {
                    duplicatePersons.add(personDto.getUid());
                } else {
                    errors.add("Exception occurred for email: " + personDto.getEmail() + " - " + e.getMessage());
                }
            }
        }

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("created", createdPersons);
        response.put("duplicates", duplicatePersons);
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Bulk extract all Person entities from the database.
     * 
     * @return A ResponseEntity containing a list of PersonDto objects.
     */
    @GetMapping("/people/bulk/extract")
    public ResponseEntity<List<PersonDto>> bulkExtractPersons() {
        // Fetch all Person entities from the database
        List<Person> people = repository.findAllByOrderByNameAsc();

        // Map Person entities to PersonDto objects
        List<PersonDto> personDtos = new ArrayList<>();
        for (Person person : people) {
            PersonDto personDto = new PersonDto();
            personDto.setEmail(person.getEmail());
            personDto.setUid(person.getUid());
            personDto.setSid(person.getSid());
            personDto.setPassword(person.getPassword()); // Optional: You may want to exclude passwords for security
                                                         // reasons
            personDto.setName(person.getName());
            personDto.setPfp(person.getPfp());
            personDto.setKasmServerNeeded(person.getKasmServerNeeded());
            personDtos.add(personDto);
        }

        // Return the list of PersonDto objects
        return new ResponseEntity<>(personDtos, HttpStatus.OK);
    }

    /**
     * Search for a Person entity by name or email.

     * 
     * @param map of a key-value (k,v), the key is "term" and the value is the
     *            search term.
     * @return A ResponseEntity containing a list of Person entities that match the
     *         search term.
     */
    @PostMapping(value = "/people/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> personSearch(@RequestBody final Map<String, String> map) {
        // extract term from RequestEntity
        String term = (String) map.get("term");

        // JPA query to filter on term
        List<Person> list = repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term);

        // return resulting list and status, error checking should be added
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @CrossOrigin(origins = { "*" })
    @GetMapping("/{sid}")
    public ResponseEntity<String> getNameById(@PathVariable String sid) {
        Person person = repository.findBySid(sid);
        if (person != null) {
            return ResponseEntity.ok(person.getName());
        } else {
            return ResponseEntity.ok("Not a valid barcode");
        }
    };
    // @PostMapping(value = "/person/setSections", produces =
    // MediaType.APPLICATION_JSON_VALUE)
    // public ResponseEntity<?> setSections(@AuthenticationPrincipal UserDetails
    // userDetails, @RequestBody final List<SectionDTO> sections) {
    // // Check if the authentication object is null
    // if (userDetails == null) {
    // return ResponseEntity
    // .status(HttpStatus.UNAUTHORIZED)
    // .body("Error: Authentication object is null. User is not authenticated.");
    // }

    // String email = userDetails.getUsername();

    // // Manually wrap the result in Optional.ofNullable
    // Optional<Person> optional =
    // Optional.ofNullable(repository.findByEmail(email));
    // if (optional.isPresent()) {
    // Person person = optional.get();

    // // Get existing sections and ensure it is not null
    // Collection<PersonSections> existingSections = person.getSections();
    // if (existingSections == null) {
    // existingSections = new ArrayList<>();
    // }

    // // Add sections
    // for (SectionDTO sectionDTO : sections) {
    // if (!existingSections.stream().anyMatch(s ->
    // s.getName().equals(sectionDTO.getName()))) {
    // PersonSections newSection = new PersonSections(sectionDTO.getName(),
    // sectionDTO.getAbbreviation(), sectionDTO.getYear());
    // existingSections.add(newSection);
    // } else {
    // return ResponseEntity
    // .status(HttpStatus.CONFLICT)
    // .body("Error: Section with name '" + sectionDTO.getName() + "' already
    // exists.");
    // }
    // }

    // // Persist updated sections
    // person.setSections(existingSections);
    // repository.save(person);

    // // Return updated Person
    // return ResponseEntity.ok(person);
    // }

    // // Person not found
    // return ResponseEntity
    // .status(HttpStatus.NOT_FOUND)
    // .body("Error: Person not found with email: " + email);
    // }

    @PutMapping("/person/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> updatePerson(Authentication authentication, @PathVariable long id,
            @RequestBody PersonDto personDto) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String actorUid = userDetails.getUsername();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        Person actorPerson = repository.findByUid(actorUid);
        if (actorPerson == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Optional<Person> optional = repository.findById(id);
        if (optional.isPresent()) { // If the person with the given ID exists
            Person existingPerson = optional.get();

            if (!isAdmin && !existingPerson.getId().equals(actorPerson.getId())) {
                logger.warn("AUDIT profile_update_blocked actor={} target={} reason=non_admin_cross_update", actorUid,
                        existingPerson.getUid());
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            boolean emailChanged = personDto.getEmail() != null
                    && !personDto.getEmail().equals(existingPerson.getEmail());
            boolean passwordChanged = personDto.getPassword() != null && !personDto.getPassword().isBlank();
            boolean uidChanged = personDto.getUid() != null && !personDto.getUid().equals(existingPerson.getUid());
            boolean nameChanged = personDto.getName() != null && !personDto.getName().equals(existingPerson.getName());
            boolean pfpChanged = personDto.getPfp() != null && !personDto.getPfp().equals(existingPerson.getPfp());
            boolean kasmChanged = personDto.getKasmServerNeeded() != null
                    && !personDto.getKasmServerNeeded().equals(existingPerson.getKasmServerNeeded());
            boolean sidChanged = personDto.getSid() != null && !personDto.getSid().equals(existingPerson.getSid());

            if (!isAdmin && uidChanged) {
                logger.warn("AUDIT profile_update_blocked actor={} target={} reason=uid_change_not_allowed", actorUid,
                        existingPerson.getUid());
                JSONObject responseObject = new JSONObject();
                responseObject.put("error", "UID cannot be changed by non-admin users");
                return new ResponseEntity<>(responseObject.toString(), HttpStatus.FORBIDDEN);
            }

            if (!isAdmin && (emailChanged || passwordChanged)) {
                if (personDto.getCurrentPassword() == null
                        || personDto.getCurrentPassword().isBlank()
                        || !passwordEncoder.matches(personDto.getCurrentPassword(), existingPerson.getPassword())) {
                    logger.warn("AUDIT profile_update_blocked actor={} target={} reason=invalid_current_password",
                            actorUid, existingPerson.getUid());
                    JSONObject responseObject = new JSONObject();
                    responseObject.put("error", "Current password is required for sensitive updates");
                    return new ResponseEntity<>(responseObject.toString(), HttpStatus.FORBIDDEN);
                }
            }

            StringBuilder changedFields = new StringBuilder();

            // Check for duplicate email if email is being changed
            if (emailChanged) {
                Person personWithEmail = repository.findByEmail(personDto.getEmail());
                if (personWithEmail != null && !personWithEmail.getId().equals(existingPerson.getId())) {
                    JSONObject responseObject = new JSONObject();
                    responseObject.put("error", "A person with email '" + personDto.getEmail() + "' already exists");
                    return new ResponseEntity<>(responseObject.toString(), HttpStatus.CONFLICT);
                }
            }

            // Check for duplicate uid if uid is being changed
            if (uidChanged) {
                Person personWithUid = repository.findByUid(personDto.getUid());
                if (personWithUid != null && !personWithUid.getId().equals(existingPerson.getId())) {
                    JSONObject responseObject = new JSONObject();
                    responseObject.put("error", "A person with uid '" + personDto.getUid() + "' already exists");
                    return new ResponseEntity<>(responseObject.toString(), HttpStatus.CONFLICT);
                }
            }

            // Update the existing person's details
            if (emailChanged) {
                existingPerson.setEmail(personDto.getEmail());
                changedFields.append("email,");
            }
            if (passwordChanged) {
                existingPerson.setPassword(passwordEncoder.encode(personDto.getPassword()));
                changedFields.append("password,");
            }
            if (nameChanged) {
                existingPerson.setName(personDto.getName());
                changedFields.append("name,");
            }
            if (isAdmin && uidChanged) {
                existingPerson.setUid(personDto.getUid());
                changedFields.append("uid,");
            }

            // Optional: Update other fields if they exist in Person
            if (pfpChanged) {
                existingPerson.setPfp(personDto.getPfp());
                changedFields.append("pfp,");
            }
            if (kasmChanged) {
                existingPerson.setKasmServerNeeded(personDto.getKasmServerNeeded());
                changedFields.append("kasmServerNeeded,");
            }
            if (sidChanged) {
                existingPerson.setSid(personDto.getSid());
                changedFields.append("sid,");
            }
            if (personDto.getFaceData() != null) {
                existingPerson.setFaceData(personDto.getFaceData());
                changedFields.append("faceData,");
            }
            // Save the updated person back to the repository
            repository.save(existingPerson);

            String changed = changedFields.length() == 0
                    ? "none"
                    : changedFields.substring(0, changedFields.length() - 1);
            logger.info("AUDIT profile_update actor={} target={} fields={} admin={}", actorUid, existingPerson.getUid(),
                    changed, isAdmin);

            // Return the updated person entity
            return new ResponseEntity<>(existingPerson, HttpStatus.OK);
        }

        // Return NOT_FOUND if the person with the given ID does not exist
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Retrieves the balance of a Person entity by its ID.
     *
     * @param id The ID of the Person entity whose balance is to be fetched.
     * @return A ResponseEntity containing the balance if found, or a NOT_FOUND
     *         status if the person does not exist.
     */
    // @GetMapping("/person/{id}/balance")
    // public ResponseEntity<Object> getBalance(@PathVariable long id) {
    // Optional<Person> optional = repository.findById(id);
    // if (optional.isPresent()) {
    // Person person = optional.get();

    // // Assuming there is a getBalance() method or a balance field in Person
    // Map<String, Object> response = new HashMap<>();
    // response.put("id", person.getId());
    // response.put("name", person.getName());
    // response.put("balance", person.getBanks().getBalance()); // Replace with
    // actual logic if needed

    // return new ResponseEntity<>(response, HttpStatus.OK);
    // }
    // return new ResponseEntity<>("Person not found", HttpStatus.NOT_FOUND);
    // }

    /**
     * Adds stats to the Person table
     * 
     * @param stat_map is a JSON object, example format:
     *                 {"health":
     *                 {"date": "2021-01-01",
     *                 "measurements":
     *                 {
     *                 "weight": "150",
     *                 "height": "70",
     *                 "bmi": "21.52"
     *                 }
     *                 }
     *                 }
     * @return A ResponseEntity containing the Person entity with updated stats, or
     *         a NOT_FOUND status if not found.
     */
    @PostMapping(value = "/person/setStats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Person> personStats(Authentication authentication,
            @RequestBody final Map<String, Object> stat_map) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // Email is mapped/unmapped to username for Spring Security

        // Find a person by username
        Optional<Person> optional = Optional.ofNullable(repository.findByUid(email));
        if (optional.isPresent()) { // Good ID
            Person person = optional.get(); // value from findByID

            // Get existing stats
            Map<String, Map<String, Object>> existingStats = person.getStats();

            // Iterate through each key in the incoming stats
            for (String key : stat_map.keySet()) {
                // Extract the stats for this key from the incoming stats
                Map<String, Object> incomingStats = (Map<String, Object>) stat_map.get(key);

                // Extract the date and attributes from the incoming stats
                String date = (String) incomingStats.get("date");
                Map<String, Object> attributeMap = new HashMap<>(incomingStats);
                attributeMap.remove("date");

                // New key test.
                if (!existingStats.containsKey(key)) {
                    // Add the new key
                    existingStats.put(key, new HashMap<>());
                }

                // Existing date test.
                if (existingStats.get(key).containsKey(date)) { // Existing date, update the attributes
                    // Make a map inside of existingStats to hold the current attributes for the
                    // date
                    Map<String, Object> existingAttributes = (Map<String, Object>) existingStats.get(key).get(date);
                    // Combine the existing attributes with these new attributes
                    existingAttributes.putAll(attributeMap);
                } else { // New date, add the new date and attributes
                    existingStats.get(key).put(date, attributeMap);
                }
            }

            // Set and save the updated stats
            person.setStats(existingStats);
            repository.save(person); // conclude by writing the stats updates to the database

            // return Person with update to Stats
            return new ResponseEntity<>(person, HttpStatus.OK);
        }
        // return Bad ID
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Search for people by name or email.
     * 
     * @param query The search query (name or email).
     * @return A ResponseEntity containing a list of Person entities that match the
     *         search query.
     */
    @GetMapping("/people/search")
    public ResponseEntity<List<Person>> searchPeople(@RequestParam("query") String query) {
        List<Person> people = repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
        return new ResponseEntity<>(people, HttpStatus.OK);
    }
}