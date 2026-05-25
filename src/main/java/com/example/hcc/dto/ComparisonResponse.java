package com.example.hcc.dto;

import com.example.hcc.entity.IcdEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.AuditorResult;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {
    private List<IcdEntry> coderCodes;
    private List<IcdEntry> auditorCodes;
    private List<IcdEntry> extraInAuditor; // Codes auditor added but coder didn't
    private List<IcdEntry> missingInAuditor; // Codes coder added but auditor didn't
    private List<IcdEntry> commonCodes;
    private CodingResult coderSubmission;
    private AuditorResult auditorSubmission;
}
