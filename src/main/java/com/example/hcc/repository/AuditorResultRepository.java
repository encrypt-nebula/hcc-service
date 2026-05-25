package com.example.hcc.repository;

import com.example.hcc.entity.AuditorResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AuditorResultRepository extends JpaRepository<AuditorResult, Long> {
    Optional<AuditorResult> findByWorkUnitId(Long workUnitId);
    Optional<AuditorResult> findByFileId(Long fileId);
    List<AuditorResult> findByAuditor_Id(Long auditorId);
}
