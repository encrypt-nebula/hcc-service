package com.example.hcc.dto;

import com.example.hcc.enums.ProjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ProjectResponse {
    private Long id;
    private String projectName;
    private ProjectType projectType;
    private String credentials;
    private String reviewMode;
    private LocalDateTime createdAt;
}
