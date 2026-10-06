package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteDetailDto {
    private Long fileId;
    private String fileName;
    private String action; // "DELETED_PERMANENTLY" or "MARKED_INACTIVE" or "NOT_FOUND"
    private String message;
}
