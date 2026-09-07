package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVsCoderVsAuditorDto {
    private String month;
    private long aiPulled;
    private long coderSubmitted;
    private long auditorVerified;
}
