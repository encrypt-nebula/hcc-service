package com.example.hcc.service;

import com.example.hcc.dto.DataExtractionDto;
import com.example.hcc.entity.*;
import com.example.hcc.enums.*;
import com.example.hcc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataExtractionService {

    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final PatientRepository patientRepository;
    private final WorkUnitRepository workUnitRepository;
    private final CodingResultRepository codingResultRepository;
    private final MeatKeywordRepository meatKeywordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processExtraction(DataExtractionDto dto) {
        // 1. Get Project
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.getProjectId()));

        // 2. Save/Update FileRecord
        FileRecord fileRecord = fileRepository.findByS3Path(dto.getS3Path())
                .orElse(new FileRecord());

        fileRecord.setProject(project);
        fileRecord.setFileName(dto.getFileName());
        fileRecord.setS3Path(dto.getS3Path());
        fileRecord.setTotalPages(dto.getTotalPages());
        fileRecord.setSignature(dto.getSignature());
        fileRecord.setUploadStatus(UploadStatus.PROCESSED);
        fileRecord = fileRepository.save(fileRecord);

        // 3. Save Patient (Primary/Top-level)
        Patient patient = new Patient();
        patient.setProject(project);
        patient.setFile(fileRecord);
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setDob(parseSafeDate(dto.getDob()));
        patient.setDateOfService(parseSafeDate(dto.getDos()));
        patient = patientRepository.save(patient);

        // 4. Handle Encounter Details (Multiple DOS)
        if (project.getProjectType() == ProjectType.PROSPECTIVE) {
            // PROSPECTIVE: Create exactly one WorkUnit for the entire file
            WorkUnit workUnit;
            if (dto.getWorkId() != null) {
                workUnit = workUnitRepository.findById(dto.getWorkId())
                        .orElse(new WorkUnit());
            } else {
                workUnit = new WorkUnit();
            }
            workUnit.setProject(project);
            workUnit.setFile(fileRecord);
            workUnit.setPatient(patient);
            workUnit.setType(WorkUnitType.PATIENT);
            workUnit.setStatus(WorkUnitStatus.UNASSIGNED);

            if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
                // Aggregate MEAT flags from all details (usually only one for prospective)
                boolean monitor = false, evaluate = false, assess = false, treat = false;
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    if (Boolean.TRUE.equals(detail.getMonitor()))
                        monitor = true;
                    if (Boolean.TRUE.equals(detail.getEvaluate()))
                        evaluate = true;
                    if (Boolean.TRUE.equals(detail.getAssessOrAddress()))
                        assess = true;
                    if (Boolean.TRUE.equals(detail.getTreat()))
                        treat = true;
                }
                workUnit.setMonitor(monitor);
                workUnit.setEvaluate(evaluate);
                workUnit.setAssessOrAddress(assess);
                workUnit.setTreat(treat);
                workUnit.setDateOfService(parseSafeDate(dto.getDetails().get(0).getDos()));


                // For PROSPECTIVE, we might want to aggregate meatValidation too, but for now
                // just store the first one or a combined version if needed.
                // Usually details has only one entry for prospective or we take the first as
                // reference.
                if (!dto.getDetails().isEmpty()) {
                    workUnit.setMeatValidation(toJson(dto.getDetails().get(0).getMeatValidation()));
                }
            }
            workUnit = workUnitRepository.save(workUnit);

            // Save MEAT Keywords
            if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
                saveMeatKeywords(workUnit, dto.getDetails().get(0));
            }

            // Create CodingResults for each detail entry, all linked to this single
            // WorkUnit
            if (dto.getDetails() != null) {
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setFile(fileRecord);
                    result.setDos(parseSafeDate(detail.getDos()));
                    result.setExtractedIcdCode(detail.getExtractedIcdCodes());
                    result.setAiIcdCode(detail.getAiSuggestedIcdCode());
                    result.setMeatValidation(toJson(detail.getMeatValidation()));
                    codingResultRepository.save(result);
                }
            }
        } else {
            // RETROPROSPECTIVE: Create one WorkUnit for each detail entry (each unique DOS)
            if (dto.getDetails() != null) {
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    WorkUnit workUnit;
                    if (dto.getWorkId() != null) {
                        workUnit = workUnitRepository.findById(dto.getWorkId())
                                .orElse(new WorkUnit());
                    } else {
                        workUnit = new WorkUnit();
                    }
                    workUnit.setProject(project);
                    workUnit.setFile(fileRecord);
                    workUnit.setPatient(patient);
                    workUnit.setType(WorkUnitType.PAGE_RANGE);
                    workUnit.setStatus(WorkUnitStatus.UNASSIGNED);

                    workUnit.setMonitor(detail.getMonitor());
                    workUnit.setEvaluate(detail.getEvaluate());
                    workUnit.setAssessOrAddress(detail.getAssessOrAddress());
                    workUnit.setTreat(detail.getTreat());
                    workUnit.setMeatValidation(toJson(detail.getMeatValidation()));
                    workUnit.setDateOfService(parseSafeDate(detail.getDos()));

                    workUnit = workUnitRepository.save(workUnit);

                    // Save MEAT Keywords
                    saveMeatKeywords(workUnit, detail);

                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setFile(fileRecord);
                    result.setDos(parseSafeDate(detail.getDos()));
                    result.setExtractedIcdCode(detail.getExtractedIcdCodes());
                    result.setAiIcdCode(detail.getAiSuggestedIcdCode());
                    result.setMeatValidation(toJson(detail.getMeatValidation()));
                    codingResultRepository.save(result);
                }
            }
        }
    }

    private void saveMeatKeywords(WorkUnit workUnit, DataExtractionDto.EncounterDetailDto detail) {
        if (detail == null)
            return;

        if (workUnit.getId() != null) {
            meatKeywordRepository.deleteByWorkUnitId(workUnit.getId());
        }

        // 1. Handle explicit fields
        saveCategoryKeywords(workUnit, "MONITORING", detail.getMonitoringKeywords());
        saveCategoryKeywords(workUnit, "EVALUATION", detail.getEvaluationKeywords());
        saveCategoryKeywords(workUnit, "ASSESSMENT", detail.getAssessmentKeywords());
        saveCategoryKeywords(workUnit, "TREATMENT", detail.getTreatmentKeywords());

        // 2. Handle map-based field (if plural list provided inside map)
        Map<String, DataExtractionDto.MeatValidationDto> meatValidation = detail.getMeatValidation();
        if (meatValidation != null) {
            meatValidation.forEach((category, data) -> {
                if (data != null && data.getKeywords() != null) {
                    saveCategoryKeywords(workUnit, category.toUpperCase(), data.getKeywords());
                }
            });
        }
    }

    private void saveCategoryKeywords(WorkUnit workUnit, String category, List<String> keywords) {
        if (keywords == null)
            return;
        for (String keyword : keywords) {
            MeatKeyword meatKeyword = MeatKeyword.builder()
                    .workUnit(workUnit)
                    .category(category)
                    .keyword(keyword)
                    .build();
            meatKeywordRepository.save(meatKeyword);
        }
    }

    private String toJson(Object obj) {
        if (obj == null)
            return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.println("Error serializing to JSON: " + e.getMessage());
            return null;
        }
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
