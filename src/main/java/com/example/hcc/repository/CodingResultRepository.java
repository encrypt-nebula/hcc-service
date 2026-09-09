package com.example.hcc.repository;

import com.example.hcc.entity.CodingResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CodingResultRepository extends JpaRepository<CodingResult, Long> {

    @Query("""
    SELECT cr.workUnit.id, COALESCE(SUM(cr.hccScore), 0)
    FROM CodingResult cr
    GROUP BY cr.workUnit.id
    """)
    List<Object[]> getRafScorePerWorkUnit();

    @Query(value = """
    SELECT cr.*
    FROM coding_results cr
    JOIN work_units wu ON cr.work_unit_id = wu.id
    JOIN JSON_TABLE(
        wu.assigned_to,
        '$[*]' COLUMNS(
            coderId INT PATH '$.id'
        )
    ) AS jt
    WHERE jt.coderId = :coderId
    """, nativeQuery = true)
    List<CodingResult> findByAssignedToCoder(@Param("coderId") Long coderId);

    void deleteByWorkUnit_Id(Long workUnitId);

    java.util.Optional<CodingResult> findByWorkUnitId(Long workUnitId);
    List<CodingResult> findByFileIdIn(List<Long> fileIds);

    @EntityGraph(attributePaths = {"file", "coder", "workUnit"})
    @Query("SELECT cr FROM CodingResult cr WHERE (:startDate IS NULL OR cr.createdAt >= :startDate) AND (:endDate IS NULL OR cr.createdAt <= :endDate)")
    List<CodingResult> findAllWithRelations(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @EntityGraph(attributePaths = {"file", "coder", "workUnit"})
    @Query("SELECT cr FROM CodingResult cr WHERE (cr.file.project.id IN :projectIds OR cr.workUnit.project.id IN :projectIds) AND (:startDate IS NULL OR cr.createdAt >= :startDate) AND (:endDate IS NULL OR cr.createdAt <= :endDate)")
    List<CodingResult> findAllByProjectIdsAndDateRange(
            @Param("projectIds") List<Long> projectIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
