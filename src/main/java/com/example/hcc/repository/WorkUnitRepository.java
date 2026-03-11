package com.example.hcc.repository;

import com.example.hcc.entity.WorkUnit;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface WorkUnitRepository extends JpaRepository<WorkUnit, Long> {

    void deleteByFile_Id(Long fileId);

    List<WorkUnit> findByFile_Id(Long fileId);

    List<WorkUnit> findByProject_Id(Long projectId);

}
