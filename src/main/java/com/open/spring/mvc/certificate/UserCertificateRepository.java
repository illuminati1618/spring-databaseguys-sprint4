package com.open.spring.mvc.certificate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCertificateRepository extends JpaRepository<UserCertificate, Long> {
    List<UserCertificate> findByPersonId(Long personId);
    List<UserCertificate> findByCertificateId(Long certificateId);
}
