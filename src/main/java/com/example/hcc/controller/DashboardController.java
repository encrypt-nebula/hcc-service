package com.example.hcc.controller;

import com.example.hcc.dto.dashboard.*;
import com.example.hcc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response.getSummary());
    }

    @GetMapping("/funnel")
    public ResponseEntity<CoderActivityFunnelDto> getFunnel(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response.getCoderActivityFunnel());
    }

    @GetMapping("/production-trend")
    public ResponseEntity<List<ProductionTrendDto>> getProductionTrend(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response.getProductionTrend());
    }

    @GetMapping("/status-breakdown")
    public ResponseEntity<FileStatusBreakdownDto> getStatusBreakdown(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, null, null);
        return ResponseEntity.ok(response.getFileStatusBreakdown());
    }

    @GetMapping("/icd-activity")
    public ResponseEntity<List<IcdActivityDto>> getIcdActivity(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response.getIcdCodeActivity());
    }

    @GetMapping("/productivity")
    public ResponseEntity<List<EmployeeProductivityDto>> getProductivity(
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        List<EmployeeProductivityDto> productivity = dashboardService.getEmployeeProductivity(companyId);
        return ResponseEntity.ok(productivity);
    }

    @GetMapping("/ai-vs-coder-auditor")
    public ResponseEntity<List<AiVsCoderVsAuditorDto>> getAiVsCoderVsAuditor(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        DashboardResponseDto response = dashboardService.getDashboardData(userId, companyId, projectId, startDate, endDate);
        return ResponseEntity.ok(response.getAiVsCoderVsAuditor());
    }

    @GetMapping("/auditor-error-rates")
    public ResponseEntity<List<AuditorErrorRateDto>> getAuditorErrorRates(
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        List<AuditorErrorRateDto> rates = dashboardService.getAuditorErrorRates(companyId);
        return ResponseEntity.ok(rates);
    }
}
