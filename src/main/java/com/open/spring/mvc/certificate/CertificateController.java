package com.open.spring.mvc.certificate;

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

import jakarta.validation.Valid;

import com.open.spring.mvc.quests.Quest;
import com.open.spring.mvc.quests.QuestsRepository;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    @Autowired
    private CertificateRepository repository;

    @Autowired
    private QuestsRepository questsRepository;

    @GetMapping
    public List<Certificate> getAllCertificates() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable Long id) {
        Optional<Certificate> certificate = repository.findById(id);
        return ResponseEntity.of(certificate);
    }

    static class CertificateRequest {
        public String title;

        public Number[] questIds;
    }

    @PostMapping("/create")
    public ResponseEntity<Certificate> createCertificate(@Valid @RequestBody CertificateRequest requestBodyCertificate) {
        if (requestBodyCertificate.title == null) {
            System.out.println("Title is required");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Quest> questRequirments = new java.util.ArrayList<>();

        if (requestBodyCertificate.questIds != null && requestBodyCertificate.questIds.length > 0) {
            List<Quest> foundQuests = questsRepository.findAllById(java.util.Arrays.stream(requestBodyCertificate.questIds).map(Number::longValue).toList());
            if (foundQuests.size() != requestBodyCertificate.questIds.length) {
                System.out.println("One or more quest IDs not found");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            questRequirments = foundQuests;
        }

        Certificate newCertificate = new Certificate(requestBodyCertificate.title, questRequirments);
        repository.save(newCertificate);
        return new ResponseEntity<>(newCertificate, HttpStatus.CREATED);
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<Certificate> updateCertificate(@PathVariable Long id, @Valid @RequestBody Certificate requestBodyCertificate) {
        Optional<Certificate> existingCertificateOpt = repository.findById(id);
        if (!existingCertificateOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Certificate existingCertificate = existingCertificateOpt.get();
        if (requestBodyCertificate.getTitle() != null) {
            existingCertificate.setTitle(requestBodyCertificate.getTitle());
        }
        if (requestBodyCertificate.getDateCreated() != null) {
            existingCertificate.setDateCreated(requestBodyCertificate.getDateCreated());
        }
        repository.save(existingCertificate);
        return new ResponseEntity<>(existingCertificate, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteCertificate(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        try {
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
