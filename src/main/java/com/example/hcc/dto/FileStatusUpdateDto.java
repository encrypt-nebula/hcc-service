package com.example.hcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileStatusUpdateDto {
    private String s3Path;
    private String status;
    private String errorMessage;

    @JsonProperty("total_pages")
    private Integer totalPages;

    private String fileName;
    private Integer projectId;
    private String projectType;
}
