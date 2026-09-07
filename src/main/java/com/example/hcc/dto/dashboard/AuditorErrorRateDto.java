package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditorErrorRateDto {
    private Long auditorId;
    private String auditorName;
    private long totalAudited;
    private long errorCount;
    private double errorRatePercentage;
}
