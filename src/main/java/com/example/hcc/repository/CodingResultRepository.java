package com.example.hcc.repository;

import com.example.hcc.entity.CodingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodingResultRepository extends JpaRepository<CodingResult, Long> {

    @Query("""
    SELECT cr.workUnit.id, COALESCE(SUM(cr.hccScore), 0)
    FROM CodingResult cr
    GROUP BY cr.workUnit.id
    """)
    List<Object[]> getRafScorePerWorkUnit();

    List<CodingResult> findByWorkUnit_AssignedTo_Id(Long userId);

    void deleteByWorkUnit_Id(Long workUnitId);

}
