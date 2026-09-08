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
    private final ProjectRepository projectRepository;
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

        // Fetch each entity table ONCE with eager JOIN FETCH (zero N+1 queries)
        List<FileRecord> allFiles = fileRepository.findAllWithProjectAndAuditor();
        List<WorkUnit> allWorkUnits = workUnitRepository.findAllWithProjectAndFile();
        List<CodingResult> allCodingResults = codingResultRepository.findAllWithRelations();
        List<AuditorResult> allAuditorResults = auditorResultRepository.findAllWithRelations();
        List<User> allUsers = userRepository.findAllWithCompany();
        List<Project> allProjects = projectRepository.findAllWithCreatedByAndCompany();

        // Build fast in-memory lookup maps (no lazy loading possible - everything is already fetched)
        Map<Long, Long> projectCompanyMap = new HashMap<>();
        for (Project p : allProjects) {
            if (p.getId() != null && p.getCreatedBy() != null && p.getCreatedBy().getCompany() != null) {
                projectCompanyMap.put(p.getId(), p.getCreatedBy().getCompany().getId());
            }
        }

        Map<Long, Long> fileProjectMap = new HashMap<>();
        Map<Long, Long> fileAuditorMap = new HashMap<>();
        for (FileRecord f : allFiles) {
            if (f.getId() != null) {
                if (f.getProject() != null) {
                    fileProjectMap.put(f.getId(), f.getProject().getId());
                }
                if (f.getAuditor() != null) {
                    fileAuditorMap.put(f.getId(), f.getAuditor().getId());
                }
            }
        }


        DashboardSummaryDto summary = getSummary(allFiles, allWorkUnits, allCodingResults, projectCompanyMap, fileProjectMap, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        CoderActivityFunnelDto funnel = getCoderActivityFunnel(allWorkUnits, allCodingResults, allUsers, projectCompanyMap, fileProjectMap, fileAuditorMap, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<ProductionTrendDto> trend = getProductionTrend(allFiles, allWorkUnits, projectCompanyMap, fileAuditorMap, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        FileStatusBreakdownDto breakdown = getFileStatusBreakdown(allWorkUnits, projectCompanyMap, fileAuditorMap, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<IcdActivityDto> icdActivity = getIcdCodeActivity(allCodingResults, allAuditorResults, projectCompanyMap, fileProjectMap, userId, effectiveRole, effectiveCompanyId, projectId, startDate, endDate);
        List<EmployeeProductivityDto> productivity = getEmployeeProductivity(allUsers, allCodingResults, allAuditorResults, projectCompanyMap, fileProjectMap, effectiveCompanyId, startDate, endDate);
        List<AiVsCoderVsAuditorDto> aiVsCoderAuditor = getAiVsCoderVsAuditor(icdActivity);
        List<AuditorErrorRateDto> errorRates = getAuditorErrorRates(allUsers, allAuditorResults, allCodingResults, projectCompanyMap, fileProjectMap, effectiveCompanyId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileProjectMap = buildFileProjectMap(fileRepository.findAllWithProjectAndAuditor());
        return getSummary(fileRepository.findAllWithProjectAndAuditor(), workUnitRepository.findAllWithProjectAndFile(), codingResultRepository.findAllWithRelations(),
                projectCompanyMap, fileProjectMap, userId, role, companyId, projectId, startDate, endDate);
    }

    public DashboardSummaryDto getSummary(
            List<FileRecord> allFiles, List<WorkUnit> allWorkUnits, List<CodingResult> allCodingResults,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<FileRecord> files = filterFiles(allFiles, projectCompanyMap, companyId, projectId, startDate, endDate);
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, projectCompanyMap, Collections.emptyMap(), userId, role, companyId, projectId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, projectCompanyMap, fileProjectMap, userId, role, companyId, projectId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileProjectMap = buildFileProjectMap(fileRepository.findAllWithProjectAndAuditor());
        Map<Long, Long> fileAuditorMap = buildFileAuditorMap(fileRepository.findAllWithProjectAndAuditor());
        return getCoderActivityFunnel(workUnitRepository.findAllWithProjectAndFile(), codingResultRepository.findAllWithRelations(), userRepository.findAllWithCompany(),
                projectCompanyMap, fileProjectMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);
    }

    public CoderActivityFunnelDto getCoderActivityFunnel(
            List<WorkUnit> allWorkUnits, List<CodingResult> allCodingResults, List<User> allUsers,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap, Map<Long, Long> fileAuditorMap,
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

        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, projectCompanyMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, projectCompanyMap, fileProjectMap, userId, role, companyId, projectId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileAuditorMap = buildFileAuditorMap(fileRepository.findAllWithProjectAndAuditor());
        return getProductionTrend(fileRepository.findAllWithProjectAndAuditor(), workUnitRepository.findAllWithProjectAndFile(), projectCompanyMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);
    }

    public List<ProductionTrendDto> getProductionTrend(
            List<FileRecord> allFiles, List<WorkUnit> allWorkUnits,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileAuditorMap,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<FileRecord> files = filterFiles(allFiles, projectCompanyMap, companyId, projectId, startDate, endDate);
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, projectCompanyMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileAuditorMap = buildFileAuditorMap(fileRepository.findAllWithProjectAndAuditor());
        return getFileStatusBreakdown(workUnitRepository.findAllWithProjectAndFile(), projectCompanyMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);
    }

    public FileStatusBreakdownDto getFileStatusBreakdown(
            List<WorkUnit> allWorkUnits,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileAuditorMap,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<WorkUnit> workUnits = filterWorkUnits(allWorkUnits, projectCompanyMap, fileAuditorMap, userId, role, companyId, projectId, startDate, endDate);
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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileProjectMap = buildFileProjectMap(fileRepository.findAllWithProjectAndAuditor());
        return getIcdCodeActivity(codingResultRepository.findAllWithRelations(), auditorResultRepository.findAllWithRelations(),
                projectCompanyMap, fileProjectMap, userId, role, companyId, projectId, startDate, endDate);
    }

    public List<IcdActivityDto> getIcdCodeActivity(
            List<CodingResult> allCodingResults, List<AuditorResult> allAuditorResults,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap,
            Long userId, Role role, Long companyId, Long projectId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, projectCompanyMap, fileProjectMap, userId, role, companyId, projectId, startDate, endDate);
        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, projectCompanyMap, fileProjectMap, userId, role, companyId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileProjectMap = buildFileProjectMap(fileRepository.findAllWithProjectAndAuditor());
        return getEmployeeProductivity(userRepository.findAllWithCompany(), codingResultRepository.findAllWithRelations(), auditorResultRepository.findAllWithRelations(),
                projectCompanyMap, fileProjectMap, companyId, startDate, endDate);
    }

    public List<EmployeeProductivityDto> getEmployeeProductivity(
            List<User> allUsers, List<CodingResult> allCodingResults, List<AuditorResult> allAuditorResults,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap,
            Long companyId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<User> users = allUsers;
        if (companyId != null) {
            users = users.stream().filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId)).collect(Collectors.toList());
        }

        List<CodingResult> codingResults = filterCodingResults(allCodingResults, projectCompanyMap, fileProjectMap, null, Role.ADMIN, companyId, null, startDate, endDate);
        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, projectCompanyMap, fileProjectMap, null, Role.ADMIN, companyId, startDate, endDate);

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
        Map<Long, Long> projectCompanyMap = buildProjectCompanyMap(projectRepository.findAllWithCreatedByAndCompany());
        Map<Long, Long> fileProjectMap = buildFileProjectMap(fileRepository.findAllWithProjectAndAuditor());
        return getAuditorErrorRates(userRepository.findAllWithCompany(), auditorResultRepository.findAllWithRelations(), codingResultRepository.findAllWithRelations(),
                projectCompanyMap, fileProjectMap, companyId, startDate, endDate);
    }

    public List<AuditorErrorRateDto> getAuditorErrorRates(
            List<User> allUsers, List<AuditorResult> allAuditorResults, List<CodingResult> allCodingResults,
            Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap,
            Long companyId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<User> auditors = allUsers.stream().filter(u -> u.getRole() == Role.AUDITOR).collect(Collectors.toList());
        if (companyId != null) {
            auditors = auditors.stream().filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId)).collect(Collectors.toList());
        }

        List<AuditorResult> auditorResults = filterAuditorResults(allAuditorResults, projectCompanyMap, fileProjectMap, null, Role.ADMIN, companyId, startDate, endDate);
        List<CodingResult> codingResults = filterCodingResults(allCodingResults, projectCompanyMap, fileProjectMap, null, Role.ADMIN, companyId, null, startDate, endDate);

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

    // Helper map builders
    private Map<Long, Long> buildProjectCompanyMap(List<Project> projects) {
        Map<Long, Long> map = new HashMap<>();
        for (Project p : projects) {
            if (p.getId() != null && p.getCreatedBy() != null && p.getCreatedBy().getCompany() != null) {
                map.put(p.getId(), p.getCreatedBy().getCompany().getId());
            }
        }
        return map;
    }

    private Map<Long, Long> buildFileProjectMap(List<FileRecord> files) {
        Map<Long, Long> map = new HashMap<>();
        for (FileRecord f : files) {
            if (f.getId() != null && f.getProject() != null) {
                map.put(f.getId(), f.getProject().getId());
            }
        }
        return map;
    }

    private Map<Long, Long> buildFileAuditorMap(List<FileRecord> files) {
        Map<Long, Long> map = new HashMap<>();
        for (FileRecord f : files) {
            if (f.getId() != null && f.getAuditor() != null) {
                map.put(f.getId(), f.getAuditor().getId());
            }
        }
        return map;
    }

    // High-performance filter methods (0 additional N+1 SQL queries)
    private List<FileRecord> filterFiles(List<FileRecord> files, Map<Long, Long> projectCompanyMap, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return files.stream().filter(f -> {
            Long pId = (f.getProject() != null) ? f.getProject().getId() : null;
            if (companyId != null) {
                Long cId = (pId != null) ? projectCompanyMap.get(pId) : null;
                if (cId == null || !companyId.equals(cId)) return false;
            }
            if (projectId != null && (pId == null || !projectId.equals(pId))) return false;
            if (start != null && f.getCreatedAt() != null && f.getCreatedAt().isBefore(start)) return false;
            if (end != null && f.getCreatedAt() != null && f.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<WorkUnit> filterWorkUnits(List<WorkUnit> units, Map<Long, Long> projectCompanyMap, Map<Long, Long> fileAuditorMap, Long userId, Role role, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return units.stream().filter(w -> {
            Long pId = (w.getProject() != null) ? w.getProject().getId() : null;
            if (companyId != null) {
                Long cId = (pId != null) ? projectCompanyMap.get(pId) : null;
                if (cId == null || !companyId.equals(cId)) return false;
            }
            if (projectId != null && (pId == null || !projectId.equals(pId))) return false;
            if (role == Role.CODER && userId != null) {
                if (w.getAssignedTo() == null || !w.getAssignedTo().contains("\"id\":" + userId)) return false;
            }
            if (role == Role.AUDITOR && userId != null) {
                Long fId = (w.getFile() != null) ? w.getFile().getId() : null;
                Long auditorId = (fId != null) ? fileAuditorMap.get(fId) : null;
                if (auditorId == null || !userId.equals(auditorId)) return false;
            }
            if (start != null && w.getCreatedAt() != null && w.getCreatedAt().isBefore(start)) return false;
            if (end != null && w.getCreatedAt() != null && w.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<CodingResult> filterCodingResults(List<CodingResult> results, Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap, Long userId, Role role, Long companyId, Long projectId, LocalDateTime start, LocalDateTime end) {
        return results.stream().filter(cr -> {
            if (role == Role.CODER && userId != null) {
                Long coderId = (cr.getCoder() != null) ? cr.getCoder().getId() : null;
                if (coderId == null || !userId.equals(coderId)) return false;
            }
            Long fId = (cr.getFile() != null) ? cr.getFile().getId() : null;
            Long pId = (fId != null) ? fileProjectMap.get(fId) : null;
            if (companyId != null) {
                Long cId = (pId != null) ? projectCompanyMap.get(pId) : null;
                if (cId == null || !companyId.equals(cId)) return false;
            }
            if (projectId != null && (pId == null || !projectId.equals(pId))) return false;
            if (start != null && cr.getCreatedAt() != null && cr.getCreatedAt().isBefore(start)) return false;
            if (end != null && cr.getCreatedAt() != null && cr.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private List<AuditorResult> filterAuditorResults(List<AuditorResult> results, Map<Long, Long> projectCompanyMap, Map<Long, Long> fileProjectMap, Long userId, Role role, Long companyId, LocalDateTime start, LocalDateTime end) {
        return results.stream().filter(ar -> {
            if (role == Role.AUDITOR && userId != null) {
                Long auditorId = (ar.getAuditor() != null) ? ar.getAuditor().getId() : null;
                if (auditorId == null || !userId.equals(auditorId)) return false;
            }
            Long fId = (ar.getFile() != null) ? ar.getFile().getId() : null;
            Long pId = (fId != null) ? fileProjectMap.get(fId) : null;
            if (companyId != null) {
                Long cId = (pId != null) ? projectCompanyMap.get(pId) : null;
                if (cId == null || !companyId.equals(cId)) return false;
            }
            if (start != null && ar.getCreatedAt() != null && ar.getCreatedAt().isBefore(start)) return false;
            if (end != null && ar.getCreatedAt() != null && ar.getCreatedAt().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }
}
