package com.open.spring.mvc.certificate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    @Query("SELECT DISTINCT c FROM Certificate c LEFT JOIN FETCH c.certificateQuests")
    List<Certificate> findAllWithQuests();
}
