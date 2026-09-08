package com.example.hcc.repository;

import com.example.hcc.entity.WorkUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface WorkUnitRepository extends JpaRepository<WorkUnit, Long> {

    void deleteByFile_Id(Long fileId);

    List<WorkUnit> findByFile_Id(Long fileId);

    List<WorkUnit> findByProject_Id(Long projectId);

    @EntityGraph(attributePaths = {"project", "file"})
    @Query("SELECT wu FROM WorkUnit wu")
    List<WorkUnit> findAllWithProjectAndFile();
}

