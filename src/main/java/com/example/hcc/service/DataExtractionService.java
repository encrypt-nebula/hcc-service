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
        patient.setHcinNumber(dto.getHcinNumber());
        patient.setMemberId(dto.getMemberId());
        patient.setPhysicianName(dto.getPhysicianName());
        patient.setSignedAt(parseSafeDate(dto.getSignedAt()));
        patient = patientRepository.save(patient);

        // 4. Handle Encounter Details (Multiple DOS)
        if (project.getProjectType() == ProjectType.PROSPECTIVE) {
            if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
                // PROSPECTIVE: Create one WorkUnit per unique valid DOS.
                Map<LocalDate, WorkUnit> workUnitsByDos = new LinkedHashMap<>();

                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    if (!isValidDetail(detail)) {
                        continue;
                    }

                    LocalDate detailDos = parseSafeDate(detail.getDos());
                    if (detailDos == null) {
                        continue;
                    }

                    WorkUnit workUnit = workUnitsByDos.get(detailDos);
                    if (workUnit == null) {
                        workUnit = new WorkUnit();
                        workUnit.setProject(project);
                        workUnit.setFile(fileRecord);
                        workUnit.setPatient(patient);
                        workUnit.setType(WorkUnitType.PATIENT);
                        workUnit.setStatus(WorkUnitStatus.UNASSIGNED);
                        workUnit.setDateOfService(detailDos);
                        workUnit = workUnitRepository.save(workUnit);
                        workUnitsByDos.put(detailDos, workUnit);
                    }

                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setFile(fileRecord);
                    result.setDos(detailDos);
                    result.setExtractedIcdCode(detail.getExtractedIcdCodes());
                    result.setAiIcdCode(
                            deduplicateAiCodes(detail.getExtractedIcdCodes(), detail.getAiSuggestedIcdCode()));
                    codingResultRepository.save(result);
                }
            }
        } else {
            // RETROPROSPECTIVE: Create one WorkUnit for each detail entry (each unique DOS)
            if (dto.getDetails() != null) {
                for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                    if (!isValidDetail(detail)) {
                        continue;
                    }

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
                    workUnit.setDateOfService(parseSafeDate(detail.getDos()));

                    workUnit = workUnitRepository.save(workUnit);

                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setFile(fileRecord);
                    result.setDos(parseSafeDate(detail.getDos()));
                    result.setExtractedIcdCode(detail.getExtractedIcdCodes());
                    result.setAiIcdCode(deduplicateAiCodes(detail.getExtractedIcdCodes(), detail.getAiSuggestedIcdCode()));
                    codingResultRepository.save(result);
                }
            }
        }
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
