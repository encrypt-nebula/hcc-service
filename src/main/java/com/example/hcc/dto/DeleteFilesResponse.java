package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFilesResponse {
    private int totalRequested;
    private int deletedCount;
    private int inactivatedCount;
    private List<FileDeleteDetailDto> details;
}
