package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProductivityDto {
    private Long userId;
    private String userName;
    private String role;
    private long completedCount;
}
