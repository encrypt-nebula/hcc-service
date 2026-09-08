package com.example.hcc.service;

import com.example.hcc.dto.dashboard.*;
import com.example.hcc.entity.*;
import com.example.hcc.enums.Role;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FileRepository fileRepository;
    private final WorkUnitRepository workUnitRepository;
    private final CodingResultRepository codingResultRepository;
    private final AuditorResultRepository auditorResultRepository;
    private final UserRepository userRepository;
    private final UserLoginLogRepository userLoginLogRepository;

    public DashboardResponseDto getDashboardData(DashboardFilterRequestDto request) {
        if (request == null) {
            request = new DashboardFilterRequestDto();
        }
        return getDashboardData(
                request.getUserId(),
                request.getCompanyId(),
                request.getProjectId(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    public DashboardResponseDto getDashboardData(
            Long userId,
            Long companyId,
            Long projectId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        User user = null;
        Role effectiveRole = Role.ADMIN;
        Long effectiveCompanyId = companyId;

        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                effectiveRole = user.getRole();
                if (user.getCompany() != null) {
                    effectiveCompanyId = user.getCompany().getId();
                }
            }
        }

        // Fetch each entity table ONCE for the entire dashboard request
        List<FileRecord> allFiles = fileRepository.findAll();
        List<WorkUnit> allWorkUnits = workUnitRepository.findAll();
        List<CodingResult> allCodingResults = codingResultRepository.findAll();
        List<AuditorResult> allAuditorResults = auditorResultRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        DashboardSummaryDto summary = getSummary(allFiles, allWorkUnits, allCodingResults, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        CoderActivityFunnelDto funnel = getCoderActivityFunnel(allWorkUnits, allCodingResults, allUsers, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<ProductionTrendDto> trend = getProductionTrend(allFiles, allWorkUnits, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        FileStatusBreakdownDto breakdown = getFileStatusBreakdown(allWorkUnits, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<IcdActivityDto> icdActivity = getIcdCodeActivity(allCodingResults, allAuditorResults, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<EmployeeProductivityDto> productivity = getEmployeeProductivity(allUsers, allCodingResults, allAuditorResults, effectiveCompanyId, startDate, endDate);
        List<AiVsCoderVsAuditorDto> aiVsCoderAuditor = getAiVsCoderVsAuditor(icdActivity);
        List<AuditorErrorRateDto> errorRates = getAuditorErrorRates(allUsers, allAuditorResults, allCodingResults, effectiveCompanyId, startDate, endDate);

        return DashboardResponseDto.builder()
                .summary(summary)
                .coderActivityFunnel(funnel)
                .productionTrend(trend)
                .fileStatusBreakdown(breakdown)
                .icdCodeActivity(icdActivity)
                .employeeProductivity(productivity)
                .aiVsCoderVsAuditor(aiVsCoderAuditor)
                .errorRateByAuditor(errorRates)
                .build();
    }

    public DashboardSummaryDto getSummary(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return getSummary(fileRepository.findAll(), workUnitRepository.findAll(), codingResultRepository.findAll(),
                userId, role, companyId, projectId, startDate, endDate);
    }

    public DashboardSummaryDto getSummary(
            List<FileRecord> allFiles, List<WorkUnit> allWorkUnits, List<CodingResult> allCodingResults,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<FileRecord> files = filterFiles(allFiles, companyId, projectId, startDate, endDate);
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, userId, role, companyId, projectId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, userId, role, companyId, projectId, startDate, endDate);

        long totalUploaded = files.size();
        long totalAssigned = workUnits.stream().filter(w -> w.getStatus() == WorkUnitStatus.ASSIGNED || w.getStatus() == WorkUnitStatus.IN_PROGRESS || w.getStatus() == WorkUnitStatus.COMPLETED).count();
        long totalCompleted = workUnits.stream().filter(w -> w.getStatus() == WorkUnitStatus.COMPLETED).count();
        long totalPending = workUnits.stream().filter(w -> w.getStatus() == WorkUnitStatus.UNASSIGNED || w.getStatus() == WorkUnitStatus.ASSIGNED || w.getStatus() == WorkUnitStatus.IN_PROGRESS).count();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long submittedToday = codingResults.stream()
                .filter(cr -> cr.getCreatedAt() != null && cr.getCreatedAt().isAfter(todayStart))
                .count();

        return DashboardSummaryDto.builder()
                .totalUploaded(totalUploaded)
                .totalAssigned(totalAssigned)
                .totalCompleted(totalCompleted)
                .totalPending(totalPending)
                .submittedToday(submittedToday)
                .build();
    }

    public CoderActivityFunnelDto getCoderActivityFunnel(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return getCoderActivityFunnel(workUnitRepository.findAll(), codingResultRepository.findAll(), userRepository.findAll(),
                userId, role, companyId, projectId, startDate, endDate);
    }

    public CoderActivityFunnelDto getCoderActivityFunnel(
            List<WorkUnit> allWorkUnits, List<CodingResult> allCodingResults, List<User> allUsers,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        long logins = 0;
        if (role == Role.CODER && userId != null) {
            logins = userLoginLogRepository.countLoginsForUserInPeriod(userId, startDate, endDate);
        } else {
            List<Long> coderIds = allUsers.stream()
                    .filter(u -> u.getRole() == Role.CODER)
                    .filter(u -> companyId == null || (u.getCompany() != null && companyId.equals(u.getCompany().getId())))
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (!coderIds.isEmpty()) {
                logins = userLoginLogRepository.countDistinctLoginsInPeriodForUsers(coderIds, startDate, endDate);
            }
        }

        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, userId, role, companyId, projectId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, userId, role, companyId, projectId, startDate, endDate);

        long assigned = workUnits.stream().filter(w -> w.getStatus() != WorkUnitStatus.UNASSIGNED).count();
        long submitted = codingResults.size();
        long pending = workUnits.stream().filter(w -> w.getStatus() == WorkUnitStatus.ASSIGNED || w.getStatus() == WorkUnitStatus.IN_PROGRESS).count();

        return CoderActivityFunnelDto.builder()
                .loggedIn(logins)
                .assigned(assigned)
                .submitted(submitted)
                .pending(pending)
                .build();
    }

    public List<ProductionTrendDto> getProductionTrend(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return getProductionTrend(fileRepository.findAll(), workUnitRepository.findAll(), userId, role, companyId, projectId, startDate, endDate);
    }

    public List<ProductionTrendDto> getProductionTrend(
            List<FileRecord> allFiles, List<WorkUnit> allWorkUnits,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<FileRecord> files = filterFiles(allFiles, companyId, projectId, startDate, endDate);
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, userId, role, companyId, projectId, startDate, endDate);

        List<YearMonth> monthsInRange = generateMonthsInRange(startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        Map<YearMonth, Long> uploadedByMonth = files.stream()
                .filter(f -> f.getCreatedAt() != null)
                .collect(Collectors.groupingBy(f -> YearMonth.from(f.getCreatedAt()), Collectors.counting()));

        Map<YearMonth, Long> completedByMonth = workUnits.stream()
                .filter(w -> w.getStatus() == WorkUnitStatus.COMPLETED && w.getCreatedAt() != null)
                .collect(Collectors.groupingBy(w -> YearMonth.from(w.getCreatedAt()), Collectors.counting()));

        List<ProductionTrendDto> result = new ArrayList<>();
        for (YearMonth ym : monthsInRange) {
            result.add(ProductionTrendDto.builder()
                    .month(ym.format(formatter))
                    .uploaded(uploadedByMonth.getOrDefault(ym, 0L))
                    .completed(completedByMonth.getOrDefault(ym, 0L))
                    .build());
        }

        return result;
    }

    public FileStatusBreakdownDto getFileStatusBreakdown(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return getFileStatusBreakdown(workUnitRepository.findAll(), userId, role, companyId, projectId, startDate, endDate);
    }

    public FileStatusBreakdownDto getFileStatusBreakdown(
            List<WorkUnit> allWorkUnits,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, userId, role, companyId, projectId, startDate, endDate);
        long total = workUnits.size();
        long completed = workUnits.stream().filter(w -> w.getStatus() == WorkUnitStatus.COMPLETED).count();
        long pending = total - completed;

        return FileStatusBreakdownDto.builder()
                .totalFiles(total)
                .completed(completed)
                .pending(pending)
                .build();
    }

    public List<IcdActivityDto> getIcdCodeActivity(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return getIcdCodeActivity(codingResultRepository.findAll(), auditorResultRepository.findAll(),
                userId, role, companyId, projectId, startDate, endDate);
    }

    public List<IcdActivityDto> getIcdCodeActivity(
            List<CodingResult> allCodingResults, List<AuditorResult> allAuditorResults,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, userId, role, companyId, projectId, startDate, endDate);
        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, userId, role, companyId, startDate, endDate);

        List<YearMonth> monthsInRange = generateMonthsInRange(startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        Map<YearMonth, Long> diagnosedMap = new HashMap<>();
        Map<YearMonth, Long> submittedMap = new HashMap<>();
        Map<YearMonth, Long> auditorVerifiedMap = new HashMap<>();

        for (CodingResult cr : codingResults) {
            if (cr.getCreatedAt() == null) continue;
            YearMonth ym = YearMonth.from(cr.getCreatedAt());

            long aiCount = (cr.getAiIcdCode() != null) ? cr.getAiIcdCode().size() : 0;
            long submittedCount = (cr.getSubmittedIcdCode() != null) ? cr.getSubmittedIcdCode().size() : 0;

            diagnosedMap.put(ym, diagnosedMap.getOrDefault(ym, 0L) + aiCount);
            submittedMap.put(ym, submittedMap.getOrDefault(ym, 0L) + submittedCount);
        }

        for (AuditorResult ar : auditorResults) {
            if (ar.getCreatedAt() == null) continue;
            YearMonth ym = YearMonth.from(ar.getCreatedAt());
            long verifiedCount = (ar.getSubmittedIcdCode() != null) ? ar.getSubmittedIcdCode().size() : 0;
            auditorVerifiedMap.put(ym, auditorVerifiedMap.getOrDefault(ym, 0L) + verifiedCount);
        }

        List<IcdActivityDto> result = new ArrayList<>();
        for (YearMonth ym : monthsInRange) {
            result.add(IcdActivityDto.builder()
                    .month(ym.format(formatter))
                    .diagnosed(diagnosedMap.getOrDefault(ym, 0L))
                    .submitted(submittedMap.getOrDefault(ym, 0L))
                    .auditorVerified(auditorVerifiedMap.getOrDefault(ym, 0L))
                    .build());
        }

        return result;
    }

    public List<EmployeeProductivityDto> getEmployeeProductivity(Long companyId, LocalDateTime startDate, LocalDateTime endDate) {
        return getEmployeeProductivity(userRepository.findAll(), codingResultRepository.findAll(), auditorResultRepository.findAll(),
                companyId, startDate, endDate);
    }

    public List<EmployeeProductivityDto> getEmployeeProductivity(
            List<User> allUsers, List<CodingResult> allCodingResults, List<AuditorResult> allAuditorResults,
            Long companyId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<User> users = allUsers;
        if (companyId != null) {
            users = users.stream().filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId)).collect(Collectors.toList());
        }

        List<CodingResult> codingResults = filterCodingResults(allCodingResults, null, Role.ADMIN, companyId, null, startDate, endDate);
        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, null, Role.ADMIN, companyId, startDate, endDate);

        Map<Long, Long> coderCounts = codingResults.stream()
                .filter(cr -> cr.getCoder() != null && cr.getCoder().getId() != null)
                .collect(Collectors.groupingBy(cr -> cr.getCoder().getId(), Collectors.counting()));

        Map<Long, Long> auditorCounts = auditorResults.stream()
                .filter(ar -> ar.getAuditor() != null && ar.getAuditor().getId() != null)
                .collect(Collectors.groupingBy(ar -> ar.getAuditor().getId(), Collectors.counting()));

        List<EmployeeProductivityDto> productivityList = new ArrayList<>();
        for (User u : users) {
            if (u.getRole() == Role.CODER) {
                long count = coderCounts.getOrDefault(u.getId(), 0L);
                productivityList.add(new EmployeeProductivityDto(u.getId(), u.getName(), u.getRole().name(), count));
            } else if (u.getRole() == Role.AUDITOR) {
                long count = auditorCounts.getOrDefault(u.getId(), 0L);
                productivityList.add(new EmployeeProductivityDto(u.getId(), u.getName(), u.getRole().name(), count));
            }
        }

        return productivityList;
    }

    public List<AiVsCoderVsAuditorDto> getAiVsCoderVsAuditor(
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<IcdActivityDto> icdActivity = getIcdCodeActivity(userId, role, companyId, projectId, startDate, endDate);
        return getAiVsCoderVsAuditor(icdActivity);
    }

    public List<AiVsCoderVsAuditorDto> getAiVsCoderVsAuditor(List<IcdActivityDto> icdActivity) {
        return icdActivity.stream().map(icd -> AiVsCoderVsAuditorDto.builder()
                .month(icd.getMonth())
                .aiPulled(icd.getDiagnosed())
                .coderSubmitted(icd.getSubmitted())
                .auditorVerified(icd.getAuditorVerified())
                .build()
        ).collect(Collectors.toList());
    }

    public List<AuditorErrorRateDto> getAuditorErrorRates(Long companyId, LocalDateTime startDate, LocalDateTime endDate) {
        return getAuditorErrorRates(userRepository.findAll(), auditorResultRepository.findAll(), codingResultRepository.findAll(),
                companyId, startDate, endDate);
    }

    public List<AuditorErrorRateDto> getAuditorErrorRates(
            List<User> allUsers, List<AuditorResult> allAuditorResults, List<CodingResult> allCodingResults,
            Long companyId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<User> auditors = allUsers.stream().filter(u -> u.getRole() == Role.AUDITOR).collect(Collectors.toList());
        if (companyId != null) {
            auditors = auditors.stream().filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId)).collect(Collectors.toList());
        }

        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, null, Role.ADMIN, companyId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, null, Role.ADMIN, companyId, null, startDate, endDate);

        Map<Long, CodingResult> codingResultMap = codingResults.stream()
                .filter(cr -> cr.getWorkUnit() != null)
                .collect(Collectors.toMap(cr -> cr.getWorkUnit().getId(), cr -> cr, (k1, k2) -> k1));

        List<AuditorErrorRateDto> errorRates = new ArrayList<>();

        for (User auditor : auditors) {
            List<AuditorResult> resultsForAuditor = auditorResults.stream()
                    .filter(ar -> ar.getAuditor() != null && auditor.getId().equals(ar.getAuditor().getId()))
                    .collect(Collectors.toList());

            long totalAudited = resultsForAuditor.size();
            long errorCount = 0;

            for (AuditorResult ar : resultsForAuditor) {
                if (ar.getWorkUnit() == null) continue;
                CodingResult cr = codingResultMap.get(ar.getWorkUnit().getId());
                if (cr != null) {
                    int coderCodeCount = (cr.getSubmittedIcdCode() != null) ? cr.getSubmittedIcdCode().size() : 0;
                    int auditorCodeCount = (ar.getSubmittedIcdCode() != null) ? ar.getSubmittedIcdCode().size() : 0;
                    if (coderCodeCount != auditorCodeCount) {
                        errorCount++;
                    }
                }
            }

            double rate = (totalAudited > 0) ? ((double) errorCount / totalAudited) * 100.0 : 0.0;
            errorRates.add(AuditorErrorRateDto.builder()
                    .auditorId(auditor.getId())
                    .auditorName(auditor.getName())
                    .totalAudited(totalAudited)
                    .errorCount(errorCount)
                    .errorRatePercentage(Math.round(rate * 100.0) / 100.0)
                    .build());
        }

        return errorRates;
    }

    private List<YearMonth> generateMonthsInRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<YearMonth> months = new ArrayList<>();
        YearMonth startYM = YearMonth.from(startDate);
        YearMonth endYM = YearMonth.from(endDate);
        while (!startYM.isAfter(endYM)) {
            months.add(startYM);
            startYM = startYM.plusMonths(1);
        }
        return months;
    }

    // Helper filter methods
    private List<FileRecord> filterFiles(List<FileRecord> files, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return files.stream().filter(f -> {
            if (companyId != null && (f.getProject() == null || f.getProject().getCreatedBy() == null || f.getProject().getCreatedBy().getCompany() == null || !companyId.equals(f.getProject().getCreatedBy().getCompany().getId()))) return false;
            if (projectId != null && (f.getProject() == null || !projectId.equals(f.getProject().getId()))) return false;
            if (start != null && f.getCreatedAt() != null && f.getCreatedAt().isBefore(start)) return false;
            if (end != null && f.getCreatedAt() != null && f.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<WorkUnit> filterWorkUnits(List<WorkUnit> units, Long userId, Role role, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return units.stream().filter(w -> {
            if (companyId != null && (w.getProject() == null || w.getProject().getCreatedBy() == null || w.getProject().getCreatedBy().getCompany() == null || !companyId.equals(w.getProject().getCreatedBy().getCompany().getId()))) return false;
            if (projectId != null && (w.getProject() == null || !projectId.equals(w.getProject().getId()))) return false;
            if (role == Role.CODER && userId != null) {
                if (w.getAssignedTo() == null || !w.getAssignedTo().contains("\"id\":" + userId)) return false;
            }
            if (role == Role.AUDITOR && userId != null) {
                if (w.getFile() == null || w.getFile().getAuditor() == null || !userId.equals(w.getFile().getAuditor().getId())) return false;
            }
            if (start != null && w.getCreatedAt() != null && w.getCreatedAt().isBefore(start)) return false;
            if (end != null && w.getCreatedAt() != null && w.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<CodingResult> filterCodingResults(List<CodingResult> results, Long userId, Role role, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return results.stream().filter(cr -> {
            if (role == Role.CODER && userId != null) {
                if (cr.getCoder() == null || !userId.equals(cr.getCoder().getId())) return false;
            }
            if (companyId != null && (cr.getFile() == null || cr.getFile().getProject() == null || cr.getFile().getProject().getCreatedBy() == null || cr.getFile().getProject().getCreatedBy().getCompany() == null || !companyId.equals(cr.getFile().getProject().getCreatedBy().getCompany().getId()))) return false;
            if (projectId != null && (cr.getFile() == null || cr.getFile().getProject() == null || !projectId.equals(cr.getFile().getProject().getId()))) return false;
            if (start != null && cr.getCreatedAt() != null && cr.getCreatedAt().isBefore(start)) return false;
            if (end != null && cr.getCreatedAt() != null && cr.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<AuditorResult> filterAuditorResults(List<AuditorResult> results, Long userId, Role role, Long companyId, LocalDateTime start, LocalDateTime end) {
        return results.stream().filter(ar -> {
            if (role == Role.AUDITOR && userId != null) {
                if (ar.getAuditor() == null || !userId.equals(ar.getAuditor().getId())) return false;
            }
            if (companyId != null && (ar.getFile() == null || ar.getFile().getProject() == null || ar.getFile().getProject().getCreatedBy() == null || ar.getFile().getProject().getCreatedBy().getCompany() == null || !companyId.equals(ar.getFile().getProject().getCreatedBy().getCompany().getId()))) return false;
            if (start != null && ar.getCreatedAt() != null && ar.getCreatedAt().isBefore(start)) return false;
            if (end != null && ar.getCreatedAt() != null && ar.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }
}
