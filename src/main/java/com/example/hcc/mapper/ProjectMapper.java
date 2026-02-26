package com.example.hcc.mapper;

import com.example.hcc.dto.ProjectResponse;
import com.example.hcc.entity.Project;

public class ProjectMapper {

    public static ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .projectType(project.getProjectType())
                .credentials(project.getCredentials())
                .reviewMode(project.getReviewMode())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
