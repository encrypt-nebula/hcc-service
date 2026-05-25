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
public class FileAuditorResultsDTO {
    private FileRecord fileRecord;
    private WorkUnit workUnit;
    private User auditor;
    private LocalDateTime createdAt;
    private BigDecimal hccScore;
    private List<CodingResultMergeDTO> auditorResults;
}
