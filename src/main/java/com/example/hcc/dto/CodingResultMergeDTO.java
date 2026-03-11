package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingResultMergeDTO {
    private Long id;
    private LocalDate dos;
    private List<String> manualIcdCode;
    private List<String> aiIcdCode;
    private List<String> extractedIcdCode;
    private List<String> submittedIcdCode;
}
