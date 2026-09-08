package com.example.hcc.repository;

import com.example.hcc.entity.AuditorResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface AuditorResultRepository extends JpaRepository<AuditorResult, Long> {
    Optional<AuditorResult> findByWorkUnitId(Long workUnitId);
    Optional<AuditorResult> findByFileId(Long fileId);
    List<AuditorResult> findByAuditor_Id(Long auditorId);
    List<AuditorResult> findByFileIdIn(List<Long> fileIds);

    @EntityGraph(attributePaths = {"file", "auditor", "workUnit"})
    @Query("SELECT ar FROM AuditorResult ar WHERE (:startDate IS NULL OR ar.createdAt >= :startDate) AND (:endDate IS NULL OR ar.createdAt <= :endDate)")
    List<AuditorResult> findAllWithRelations(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
