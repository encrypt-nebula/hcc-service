package com.example.hcc.service;

import com.example.hcc.dto.DataExtractionDto;
import com.example.hcc.entity.*;
import com.example.hcc.enums.*;
import com.example.hcc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataExtractionService {

    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final PatientRepository patientRepository;
    private final WorkUnitRepository workUnitRepository;
    private final CodingResultRepository codingResultRepository;
    private final MeatKeywordRepository meatKeywordRepository;

    @Transactional
    public void processExtraction(DataExtractionDto dto) {
        System.out.println("Processing extraction for project: " + dto.getProjectId() + ", file: " + dto.getFileName());

        // 1. Get Project
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.getProjectId()));

        // 2. Clear existing entries if reprocessing (allows clean state)
        fileRepository.findByS3Path(dto.getS3Path()).ifPresent(f -> {
            List<WorkUnit> existingWus = workUnitRepository.findByFile_Id(f.getId());
            for (WorkUnit wu : existingWus) {
                meatKeywordRepository.deleteByWorkUnitId(wu.getId());
                codingResultRepository.deleteByWorkUnit_Id(wu.getId());
            }
            workUnitRepository.deleteByFile_Id(f.getId());
            patientRepository.deleteByFile_Id(f.getId());
        });

        // 3. Save/Update FileRecord
        FileRecord fileRecord = fileRepository.findByS3Path(dto.getS3Path())
                .orElse(new FileRecord());

        fileRecord.setProject(project);
        fileRecord.setFileName(dto.getFileName());
        fileRecord.setS3Path(dto.getS3Path());
        fileRecord.setTotalPages(dto.getTotalPages());
        fileRecord.setSignature(dto.getSignature());
        fileRecord.setIsValid(dto.getIsValid());
        fileRecord.setUploadStatus(dto.getIsValid() != null && !dto.getIsValid() ? UploadStatus.INVALID : UploadStatus.PROCESSED);
        if (fileRecord.getStatus() == null) {
            fileRecord.setStatus(Status.ACTIVE);
        }
        fileRecord = fileRepository.save(fileRecord);

        // 4. Save Patient (Primary/Top-level)
        Patient patient = new Patient();
        patient.setProject(project);
        patient.setFile(fileRecord);
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setDob(parseSafeDate(dto.getDob()));
        patient.setDateOfService(parseSafeDate(dto.getDos()));
        patient.setHcinNumber(dto.getHcinNumber());
        patient.setMemberId(dto.getMemberId());
        patient.setPhysicianName(dto.getPhysicianName());
        patient.setInsurance(dto.getInsurance());
        patient.setSignedAt(parseSafeDate(dto.getSignedAt()));
        patient = patientRepository.save(patient);

        // 5. Handle Encounter Details
        boolean workUnitCreated = false;

        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            if (project.getProjectType() == ProjectType.PROSPECTIVE) {
                // PROSPECTIVE: Create one WorkUnit per unique valid DOS.
                Map<LocalDate, WorkUnit> workUnitsByDos = new LinkedHashMap<>();

                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    if (!isValidDetail(detail)) continue;

                    LocalDate detailDos = parseSafeDate(detail.getDos());
                    if (detailDos == null) continue;

                    WorkUnit workUnit = workUnitsByDos.get(detailDos);
                    if (workUnit == null) {
                        workUnit = new WorkUnit();
                        workUnit.setProject(project);
                        workUnit.setFile(fileRecord);
                        workUnit.setPatient(patient);
                        workUnit.setType(WorkUnitType.PATIENT);
                        workUnit.setStatus(WorkUnitStatus.UNASSIGNED);
                        workUnit.setDateOfService(detailDos);
                        applyMeatDetails(workUnit, detail);
                        workUnit = workUnitRepository.save(workUnit);

                        workUnitsByDos.put(detailDos, workUnit);
                        workUnitCreated = true;
                    } else {
                        applyMeatDetails(workUnit, detail);
                        workUnit = workUnitRepository.save(workUnit);
                    }
                    saveMeatKeywords(workUnit, detail);
                    saveCodingResult(workUnit, fileRecord, detailDos, detail);
                }
            } else {
                // RETROPROSPECTIVE: Create one WorkUnit for each detail entry
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    if (!isValidDetail(detail)) continue;

                    LocalDate detailDos = parseSafeDate(detail.getDos());
                    WorkUnit workUnit = new WorkUnit();
                    workUnit.setProject(project);
                    workUnit.setFile(fileRecord);
                    workUnit.setPatient(patient);
                    workUnit.setType(WorkUnitType.PAGE_RANGE);
                    workUnit.setStatus(WorkUnitStatus.UNASSIGNED);
                    workUnit.setDateOfService(detailDos);
                    applyMeatDetails(workUnit, detail);
                    workUnit = workUnitRepository.save(workUnit);

                    saveMeatKeywords(workUnit, detail);
                    saveCodingResult(workUnit, fileRecord, detailDos, detail);
                    workUnitCreated = true;
                }
            }
        }

        // 6. Fallback: If no work units were created (empty details or all invalid), create a default one
        if (!workUnitCreated) {
            System.out.println("No encounter details found. Creating a fallback work unit for file: " + dto.getFileName());
            WorkUnit fallbackWu = new WorkUnit();
            fallbackWu.setProject(project);
            fallbackWu.setFile(fileRecord);
            fallbackWu.setPatient(patient);
            fallbackWu.setType(project.getProjectType() == ProjectType.PROSPECTIVE ? WorkUnitType.PATIENT : WorkUnitType.PAGE_RANGE);
            fallbackWu.setStatus(WorkUnitStatus.UNASSIGNED);
            fallbackWu.setDateOfService(parseSafeDate(dto.getDos()));
            fallbackWu.setMonitor(Boolean.TRUE.equals(dto.getMonitor()));
            fallbackWu.setEvaluate(Boolean.TRUE.equals(dto.getEvaluate()));
            fallbackWu.setAssessOrAddress(Boolean.TRUE.equals(dto.getAssessOrAddress()));
            fallbackWu.setTreat(Boolean.TRUE.equals(dto.getTreat()));
            fallbackWu = workUnitRepository.save(fallbackWu);

            saveCategoryKeywords(fallbackWu, "MONITORING", dto.getMonitoringKeywords());
            saveCategoryKeywords(fallbackWu, "EVALUATION", dto.getEvaluationKeywords());
            saveCategoryKeywords(fallbackWu, "ASSESSMENT", dto.getAssessmentKeywords());
            saveCategoryKeywords(fallbackWu, "TREATMENT", dto.getTreatmentKeywords());
        }
    }

    private void applyMeatDetails(WorkUnit workUnit, DataExtractionDto.EncounterDetailDto detail) {
        if (detail == null || workUnit == null) return;
        boolean hasMonitor = Boolean.TRUE.equals(workUnit.getMonitor())
                || Boolean.TRUE.equals(detail.getMonitor())
                || (detail.getMonitoringKeywords() != null && !detail.getMonitoringKeywords().isEmpty());

        boolean hasEvaluate = Boolean.TRUE.equals(workUnit.getEvaluate())
                || Boolean.TRUE.equals(detail.getEvaluate())
                || (detail.getEvaluationKeywords() != null && !detail.getEvaluationKeywords().isEmpty());

        boolean hasAssess = Boolean.TRUE.equals(workUnit.getAssessOrAddress())
                || Boolean.TRUE.equals(detail.getAssessOrAddress())
                || (detail.getAssessmentKeywords() != null && !detail.getAssessmentKeywords().isEmpty());

        boolean hasTreat = Boolean.TRUE.equals(workUnit.getTreat())
                || Boolean.TRUE.equals(detail.getTreat())
                || (detail.getTreatmentKeywords() != null && !detail.getTreatmentKeywords().isEmpty());

        workUnit.setMonitor(hasMonitor);
        workUnit.setEvaluate(hasEvaluate);
        workUnit.setAssessOrAddress(hasAssess);
        workUnit.setTreat(hasTreat);
    }

    private void saveMeatKeywords(WorkUnit workUnit, DataExtractionDto.EncounterDetailDto detail) {
        if (detail == null || workUnit == null || workUnit.getId() == null) return;

        saveCategoryKeywords(workUnit, "MONITORING", detail.getMonitoringKeywords());
        saveCategoryKeywords(workUnit, "EVALUATION", detail.getEvaluationKeywords());
        saveCategoryKeywords(workUnit, "ASSESSMENT", detail.getAssessmentKeywords());
        saveCategoryKeywords(workUnit, "TREATMENT", detail.getTreatmentKeywords());

        if (detail.getMeatKeywords() != null) {
            detail.getMeatKeywords().forEach((cat, kws) -> {
                if (kws != null && !kws.isEmpty() && cat != null) {
                    saveCategoryKeywords(workUnit, cat.toUpperCase().trim(), kws);
                }
            });
        }
    }

    private void saveCategoryKeywords(WorkUnit workUnit, String category, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return;
        for (String kw : keywords) {
            if (kw == null || kw.trim().isEmpty()) continue;
            MeatKeyword mk = MeatKeyword.builder()
                    .workUnit(workUnit)
                    .category(category)
                    .keyword(kw.trim())
                    .build();
            meatKeywordRepository.save(mk);
        }
    }

    private void saveCodingResult(WorkUnit wu, FileRecord file, LocalDate dos, DataExtractionDto.EncounterDetailDto detail) {
        CodingResult result = new CodingResult();
        result.setWorkUnit(wu);
        result.setFile(file);
        result.setDos(dos);
        result.setExtractedIcdCode(detail.getExtractedIcdCodes());
        result.setAiIcdCode(deduplicateAiCodes(detail.getExtractedIcdCodes(), detail.getAiSuggestedIcdCode()));
        codingResultRepository.save(result);
    }

    /**
     * Removes any ICD codes from the AI suggested list that are already present
     * in the extracted ICD codes list.
     */
    private List<IcdEntry> deduplicateAiCodes(List<IcdEntry> extractedCodes, List<IcdEntry> aiSuggestedCodes) {
        if (aiSuggestedCodes == null || aiSuggestedCodes.isEmpty()) {
            return aiSuggestedCodes;
        }
        if (extractedCodes == null || extractedCodes.isEmpty()) {
            return aiSuggestedCodes;
        }
        Set<String> extractedSet = new HashSet<>(extractedCodes.stream().map(IcdEntry::getCode).toList());
        List<IcdEntry> deduplicated = new ArrayList<>();
        for (IcdEntry entry : aiSuggestedCodes) {
            if (!extractedSet.contains(entry.getCode())) {
                deduplicated.add(entry);
            }
        }
        return deduplicated;
    }

    private boolean isValidDetail(DataExtractionDto.EncounterDetailDto detail) {
        if (detail == null)
            return false;

        boolean isDosPresent = detail.getDos() != null && !detail.getDos().trim().isEmpty()
                && !detail.getDos().equalsIgnoreCase("Unknown");

        // Valid only if DOS is present.
        return isDosPresent;
    }

    private LocalDate parseSafeDate(String dateStr) {
        if (dateStr == null || dateStr.equalsIgnoreCase("Unknown") || dateStr.isBlank()) {
            return null;
        }

        // Try multiple formats
        String[] formats = { "yyyy-MM-dd", "MM/dd/yyyy", "M/d/yyyy" };

        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern(format));
            } catch (Exception ignored) {
            }
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateStr + ". Error: " + e.getMessage());
            return null;
        }
    }
}
