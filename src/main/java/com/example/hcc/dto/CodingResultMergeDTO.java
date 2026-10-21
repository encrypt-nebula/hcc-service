package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.hcc.entity.IcdEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingResultMergeDTO {
    private Long id;
    private LocalDate dos;
    private List<IcdEntry> manualIcdCode;
    private List<IcdEntry> aiIcdCode;
    private List<IcdEntry> extractedIcdCode;
    private List<IcdEntry> submittedIcdCode;
    private Boolean monitor;
    private Boolean evaluate;
    private Boolean assessOrAddress;
    private Boolean treat;
    private Map<String, List<String>> meatKeywords;
}
