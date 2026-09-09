package com.example.hcc.repository;

import com.example.hcc.entity.FileRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface FileRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByS3Path(String s3Path);
    List<FileRecord> findByAuditor_Id(Long auditorId);

    @EntityGraph(attributePaths = {"project", "auditor"})
    @Query("SELECT f FROM FileRecord f WHERE (:startDate IS NULL OR f.createdAt >= :startDate) AND (:endDate IS NULL OR f.createdAt <= :endDate)")
    List<FileRecord> findAllWithProjectAndAuditor(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @EntityGraph(attributePaths = {"project", "auditor"})
    @Query("SELECT f FROM FileRecord f WHERE f.project.id IN :projectIds AND (:startDate IS NULL OR f.createdAt >= :startDate) AND (:endDate IS NULL OR f.createdAt <= :endDate)")
    List<FileRecord> findAllByProjectIdsAndDateRange(
            @Param("projectIds") List<Long> projectIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
