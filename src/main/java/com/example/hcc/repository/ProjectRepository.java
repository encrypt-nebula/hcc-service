package com.example.hcc.repository;

import com.example.hcc.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Modifying
    @Query(
            value = """
            UPDATE projects\s
            SET status = 'INACTIVE'\s
            WHERE id IN (:ids)
       \s""",
            nativeQuery = true
    )
    int markInactiveByIds(@Param("ids") List<Long> ids);
}

