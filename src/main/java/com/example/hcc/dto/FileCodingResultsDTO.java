package com.example.hcc.dto;

import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.User;
import com.example.hcc.entity.WorkUnit;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileCodingResultsDTO {
    private FileRecord fileRecord;
    private WorkUnit workUnit; // common for all
    private User coder;        // common
    private String meatValidation; // common
    private LocalDateTime createdAt; // common
    private BigDecimal hccScore;
    private List<CodingResultMergeDTO> codingResults; // different columns merged
}
