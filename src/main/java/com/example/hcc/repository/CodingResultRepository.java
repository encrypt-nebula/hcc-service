package com.example.hcc.repository;

import com.example.hcc.entity.CodingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CodingResultRepository extends JpaRepository<CodingResult, Long> {

    @Query("""
    SELECT cr.workUnit.id, COALESCE(SUM(cr.hccScore), 0)
    FROM CodingResult cr
    GROUP BY cr.workUnit.id
    """)
    List<Object[]> getRafScorePerWorkUnit();

    //List<CodingResult> findByWorkUnit_AssignedTo_Id(Long userId);
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

}
