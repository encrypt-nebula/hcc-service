package com.example.hcc.controller;

import com.example.hcc.dto.dashboard.*;
import com.example.hcc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getSummary());
    }

    @PostMapping("/funnel")
    public ResponseEntity<CoderActivityFunnelDto> getFunnel(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getCoderActivityFunnel());
    }

    @PostMapping("/production-trend")
    public ResponseEntity<List<ProductionTrendDto>> getProductionTrend(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getProductionTrend());
    }

    @PostMapping("/status-breakdown")
    public ResponseEntity<FileStatusBreakdownDto> getStatusBreakdown(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getFileStatusBreakdown());
    }

    @PostMapping("/icd-activity")
    public ResponseEntity<List<IcdActivityDto>> getIcdActivity(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getIcdCodeActivity());
    }

    @PostMapping("/productivity")
    public ResponseEntity<List<EmployeeProductivityDto>> getProductivity(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        List<EmployeeProductivityDto> productivity = dashboardService.getEmployeeProductivity(
                request.getCompanyId(), request.getStartDate(), request.getEndDate()
        );
        return ResponseEntity.ok(productivity);
    }

    @PostMapping("/ai-vs-coder-auditor")
    public ResponseEntity<List<AiVsCoderVsAuditorDto>> getAiVsCoderVsAuditor(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        DashboardResponseDto response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response.getAiVsCoderVsAuditor());
    }

    @PostMapping("/auditor-error-rates")
    public ResponseEntity<List<AuditorErrorRateDto>> getAuditorErrorRates(
            @RequestBody(required = false) DashboardFilterRequestDto request
    ) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        List<AuditorErrorRateDto> rates = dashboardService.getAuditorErrorRates(
                request.getCompanyId(), request.getStartDate(), request.getEndDate()
        );
        return ResponseEntity.ok(rates);
    }
}
