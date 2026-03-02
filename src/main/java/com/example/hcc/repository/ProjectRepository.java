package com.example.hcc.repository;

import com.example.hcc.entity.Project;
import com.example.hcc.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Modifying
    @Query(
            value = """
        UPDATE projects
        SET status = :status
        WHERE id IN (:ids)
    """,
            nativeQuery = true
    )
    int bulkStatusUpdate(
            @Param("ids") List<Long> ids,
            @Param("status") String status
    );

    List<Project> findAllByStatus(Status status);

}

