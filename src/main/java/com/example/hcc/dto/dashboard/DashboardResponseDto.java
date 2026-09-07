package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private DashboardSummaryDto summary;
    private CoderActivityFunnelDto coderActivityFunnel;
    private List<ProductionTrendDto> productionTrend;
    private FileStatusBreakdownDto fileStatusBreakdown;
    private List<IcdActivityDto> icdCodeActivity;
    private List<EmployeeProductivityDto> employeeProductivity;
    private List<AiVsCoderVsAuditorDto> aiVsCoderVsAuditor;
    private List<AuditorErrorRateDto> errorRateByAuditor;
}
