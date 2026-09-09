package com.example.hcc.repository;

import com.example.hcc.entity.WorkUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkUnitRepository extends JpaRepository<WorkUnit, Long> {

    void deleteByFile_Id(Long fileId);

    List<WorkUnit> findByFile_Id(Long fileId);

    List<WorkUnit> findByProject_Id(Long projectId);

    @EntityGraph(attributePaths = {"project", "file"})
    @Query("SELECT wu FROM WorkUnit wu WHERE (:startDate IS NULL OR wu.createdAt >= :startDate) AND (:endDate IS NULL OR wu.createdAt <= :endDate)")
    List<WorkUnit> findAllWithProjectAndFile(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @EntityGraph(attributePaths = {"project", "file"})
    @Query("SELECT wu FROM WorkUnit wu WHERE wu.project.id IN :projectIds AND (:startDate IS NULL OR wu.createdAt >= :startDate) AND (:endDate IS NULL OR wu.createdAt <= :endDate)")
    List<WorkUnit> findAllByProjectIdsAndDateRange(
            @Param("projectIds") List<Long> projectIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
