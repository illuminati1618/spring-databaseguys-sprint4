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

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    @Autowired
    private CertificateRepository repository;

    @GetMapping
    public List<Certificate> getAllCertificates() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable Long id) {
        Optional<Certificate> certificate = repository.findById(id);
        return ResponseEntity.of(certificate);
    }

    @PostMapping("/create")
    public ResponseEntity<Certificate> createCertificate(@Valid @RequestBody Certificate requestBodyCertificate) {
        if (requestBodyCertificate.getTitle() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Certificate newCertificate = new Certificate(requestBodyCertificate.getTitle());
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
