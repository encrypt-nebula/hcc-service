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
import java.util.List;

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

        // 2. Save FileRecord
        FileRecord fileRecord = new FileRecord();
        fileRecord.setProject(project);
        fileRecord.setFileName(dto.getFileName());
        fileRecord.setS3Path(dto.getS3Path());
        fileRecord.setTotalPages(dto.getTotalPages());
        fileRecord.setUploadStatus(UploadStatus.PROCESSED);
        fileRecord = fileRepository.save(fileRecord);

        // 3. Save Patient (Primary/Top-level)
        Patient patient = new Patient();
        patient.setProject(project);
        patient.setFile(fileRecord);
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        if (dto.getDob() != null && !dto.getDob().equalsIgnoreCase("Unknown")) {
            patient.setDob(LocalDate.parse(dto.getDob()));
        }
        if (dto.getDos() != null && !dto.getDos().equalsIgnoreCase("Unknown")) {
            patient.setDateOfService(LocalDate.parse(dto.getDos()));
        }
        patient = patientRepository.save(patient);

        // 4. Handle Encounter Details (Multiple DOS)
        if (dto.getDetails() != null) {
            for (DataExtractionDto.EncounterDetailDto detail : dto.getDetails()) {
                // For each unique DOS, create a WorkUnit
                WorkUnit workUnit = new WorkUnit();
                workUnit.setProject(project);
                workUnit.setFile(fileRecord);
                workUnit.setPatient(patient);
                workUnit.setType(dto.getWorkUnitType().equalsIgnoreCase("PATIENT") ? WorkUnitType.PATIENT : WorkUnitType.PAGE_RANGE);
                workUnit.setStatus(WorkUnitStatus.UNASSIGNED);
                workUnit = workUnitRepository.save(workUnit);

                // Save Coding Results for this WorkUnit
                List<String> allCodes = new ArrayList<>();
                if (detail.getExtractedIcdCodes() != null) allCodes.addAll(detail.getExtractedIcdCodes());
                if (detail.getAiSuggestedIcdCode() != null) allCodes.addAll(detail.getAiSuggestedIcdCode());

                for (String code : allCodes) {
                    CodingResult result = new CodingResult();
                    result.setWorkUnit(workUnit);
                    result.setIcdCode(code);
                    result.setCodingSource(CodingSource.AI);
                    codingResultRepository.save(result);
                }
            }
        }
    }
}
