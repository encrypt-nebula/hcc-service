package com.example.hcc.mapper;

import com.example.hcc.dto.ExtractDataRequest;
import com.example.hcc.entity.*;
import com.example.hcc.enums.UploadStatus;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExtractDataMapper {

    public FileRecord mapFileRecord(ExtractDataRequest req, Project project) {
        return FileRecord.builder()
                .project(project)
                .fileName(req.getFileName())
                .s3Path(req.getS3Path())
                .totalPages(req.getTotalPages())
                .signature(req.getSignature())
                .uploadStatus(UploadStatus.PROCESSED)
                .build();
    }

    public Patient mapPatient(ExtractDataRequest req,
                              Project project,
                              FileRecord file) {

        return Patient.builder()
                .project(project)
                .file(file)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .dob(req.getDob())
                .dateOfService(req.getDos())
                .build();
    }

    public WorkUnit mapWorkUnit(ExtractDataRequest req,
                                Project project,
                                FileRecord file,
                                Patient patient) {

        return WorkUnit.builder()
                .project(project)
                .file(file)
                .patient(patient)
                .type(WorkUnitType.valueOf(req.getWorkUnitType()))
                .status(WorkUnitStatus.UNASSIGNED)
                .build();
    }

    public CodingResult mapCodingResult(WorkUnit workUnit,
                                        ExtractDataRequest.ExtractDetail detail) {

        return CodingResult.builder()
                .workUnit(workUnit)
                .extractedIcdCode(detail.getExtractedIcdCodes())
                .aiIcdCode(deduplicateAiCodes(detail.getExtractedIcdCodes(), detail.getAiSuggestedIcdCode()))
                .build();
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
}