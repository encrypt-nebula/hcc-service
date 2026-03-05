package com.example.hcc.service;

import com.example.hcc.dto.DataExtractionDto;
import com.example.hcc.entity.*;
import com.example.hcc.enums.*;
import com.example.hcc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DataExtractionService {

    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final PatientRepository patientRepository;
    private final WorkUnitRepository workUnitRepository;
    private final CodingResultRepository codingResultRepository;

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
            WorkUnit workUnit = new WorkUnit();
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
            }
            workUnit = workUnitRepository.save(workUnit);

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
                    codingResultRepository.save(result);
                }
            }
        } else {
            // RETROPROSPECTIVE: Create one WorkUnit for each detail entry (each unique DOS)
            if (dto.getDetails() != null) {
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    WorkUnit workUnit = new WorkUnit();
                    workUnit.setProject(project);
                    workUnit.setFile(fileRecord);
                    workUnit.setPatient(patient);
                    workUnit.setType(WorkUnitType.PAGE_RANGE);
                    workUnit.setStatus(WorkUnitStatus.UNASSIGNED);

                    workUnit.setMonitor(detail.getMonitor());
                    workUnit.setEvaluate(detail.getEvaluate());
                    workUnit.setAssessOrAddress(detail.getAssessOrAddress());
                    workUnit.setTreat(detail.getTreat());
                    workUnit = workUnitRepository.save(workUnit);

                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setFile(fileRecord);
                    result.setDos(parseSafeDate(detail.getDos()));
                    result.setExtractedIcdCode(detail.getExtractedIcdCodes());
                    result.setAiIcdCode(detail.getAiSuggestedIcdCode());
                    codingResultRepository.save(result);
                }
            }
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
