package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcdActivityDto {
    private String month;
    private long diagnosed;
    private long submitted;
    private long auditorVerified;
}
