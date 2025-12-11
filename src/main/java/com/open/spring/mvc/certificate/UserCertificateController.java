package com.open.spring.mvc.certificate;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

import jakarta.validation.Valid;

class UserCertificateRequestBody {
    public Long personId;
    public Long certificateId;
}

@RestController
@RequestMapping("/api/user-certificates")
public class UserCertificateController {

    @Autowired
    private UserCertificateRepository userCertificateRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @GetMapping
    public List<UserCertificate> getAllUserCertificates() {
        return userCertificateRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserCertificate> getUserCertificate(@PathVariable Long id) {
        Optional<UserCertificate> userCertificate = userCertificateRepository.findById(id);
        return ResponseEntity.of(userCertificate);
    }

    @GetMapping("/person/{personId}")
    public List<UserCertificate> getCertificatesByPersonId(@PathVariable Long personId) {
        return userCertificateRepository.findByPersonId(personId);
    }

    @GetMapping("/certificate/{certificateId}")
    public List<UserCertificate> getUsersByCertificateId(@PathVariable Long certificateId) {
        return userCertificateRepository.findByCertificateId(certificateId);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUserCertificate(@Valid @RequestBody UserCertificateRequestBody requestBody) {
        if (requestBody.personId == null || requestBody.certificateId == null) {
            return new ResponseEntity<>("Person ID and Certificate ID must be provided", HttpStatus.BAD_REQUEST);
        }

        Optional<Person> personOpt = personRepository.findById(requestBody.personId);
        if (!personOpt.isPresent()) {
            return new ResponseEntity<>("Person not found", HttpStatus.NOT_FOUND);
        }

        Optional<Certificate> certificateOpt = certificateRepository.findById(requestBody.certificateId);
        if (!certificateOpt.isPresent()) {
            return new ResponseEntity<>("Certificate not found", HttpStatus.NOT_FOUND);
        }

        UserCertificate newUserCertificate = new UserCertificate(personOpt.get(), certificateOpt.get(), new Date());
        userCertificateRepository.save(newUserCertificate);
        return new ResponseEntity<>(newUserCertificate, HttpStatus.CREATED);
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUserCertificate(@PathVariable Long id, @Valid @RequestBody UserCertificateRequestBody requestBody) {
        Optional<UserCertificate> existingUserCertificateOpt = userCertificateRepository.findById(id);
        if (!existingUserCertificateOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        UserCertificate existingUserCertificate = existingUserCertificateOpt.get();

        if (requestBody.personId != null) {
            Optional<Person> personOpt = personRepository.findById(requestBody.personId);
            if (!personOpt.isPresent()) {
                return new ResponseEntity<>("Person not found", HttpStatus.NOT_FOUND);
            }
            existingUserCertificate.setPerson(personOpt.get());
        }

        if (requestBody.certificateId != null) {
            Optional<Certificate> certificateOpt = certificateRepository.findById(requestBody.certificateId);
            if (!certificateOpt.isPresent()) {
                return new ResponseEntity<>("Certificate not found", HttpStatus.NOT_FOUND);
            }
            existingUserCertificate.setCertificate(certificateOpt.get());
        }
        
        userCertificateRepository.save(existingUserCertificate);
        return new ResponseEntity<>(existingUserCertificate, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteUserCertificate(@PathVariable Long id) {
        if (!userCertificateRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        try {
            userCertificateRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
