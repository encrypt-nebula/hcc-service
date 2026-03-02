package com.example.hcc.mapper;

import com.example.hcc.dto.ExtractDataRequest;
import com.example.hcc.entity.*;
import com.example.hcc.enums.UploadStatus;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import org.springframework.stereotype.Component;

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
                .aiIcdCode(detail.getAiSuggestedIcdCode())
                .build();
    }
}