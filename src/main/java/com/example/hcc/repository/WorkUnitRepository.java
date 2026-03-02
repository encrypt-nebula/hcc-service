package com.example.hcc.repository;

import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.WorkUnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkUnitRepository extends JpaRepository<WorkUnit, Long> {
    @Modifying
    @Query("""
        UPDATE WorkUnit w
        SET w.assignedTo.id = :coderId,
            w.status = 'ASSIGNED'
        WHERE w.id IN :workUnitIds
    """)
    int assignWorkUnits(
            @Param("coderId") Long coderId,
            @Param("workUnitIds") List<Long> workUnitIds
    );
    List<WorkUnit> findByAssignedToIdAndStatus(Long coderId, WorkUnitStatus status);

    void deleteByFile_Id(Long fileId);

    List<WorkUnit> findByFile_Id(Long fileId);

    List<WorkUnit> findByProject_Id(Long projectId);

}
