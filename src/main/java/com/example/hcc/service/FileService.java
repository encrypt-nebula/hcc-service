package com.example.hcc.service;

import com.example.hcc.dto.DeleteFilesResponse;
import com.example.hcc.dto.FileDeleteDetailDto;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.Status;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository repo;
    private final WorkUnitRepository workUnitRepo;
    private final CodingResultRepository codingResultRepo;
    private final PatientRepository patientRepo;
    private final AuditorResultRepository auditorResultRepo;
    private final FileProcessingStatusRepository fileProcessingStatusRepo;
    private final S3StorageService s3StorageService;

    public FileRecord create(FileRecord fileRecord) {
        if (fileRecord.getStatus() == null) {
            fileRecord.setStatus(Status.ACTIVE);
        }
        return repo.save(fileRecord);
    }

    public List<FileRecord> getAll() {
        return getAll("ACTIVE");
    }

    public List<FileRecord> getAll(String statusParam) {
        if (statusParam == null || statusParam.isBlank() || "ACTIVE".equalsIgnoreCase(statusParam)) {
            return repo.findAllByStatus(Status.ACTIVE);
        } else if ("INACTIVE".equalsIgnoreCase(statusParam)) {
            return repo.findAllByStatus(Status.INACTIVE);
        } else if ("ALL".equalsIgnoreCase(statusParam)) {
            return repo.findAll();
        } else {
            try {
                Status status = Status.valueOf(statusParam.toUpperCase());
                return repo.findAllByStatus(status);
            } catch (IllegalArgumentException e) {
                return repo.findAllByStatus(Status.ACTIVE);
            }
        }
    }

    public FileRecord get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    public FileRecord update(Long id, FileRecord incoming) {

        FileRecord existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (incoming.getProject() != null) {
            existing.setProject(incoming.getProject());
        }

        if (incoming.getFileName() != null) {
            existing.setFileName(incoming.getFileName());
        }

        if (incoming.getS3Path() != null) {
            existing.setS3Path(incoming.getS3Path());
        }

        if (incoming.getTotalPages() != null) {
            existing.setTotalPages(incoming.getTotalPages());
        }

        if (incoming.getUploadStatus() != null) {
            existing.setUploadStatus(incoming.getUploadStatus());
        }

        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }

        if (incoming.getSignature() != null) {
            existing.setSignature(incoming.getSignature());
        }

        if (incoming.getAuditor() != null) {
            existing.setAuditor(incoming.getAuditor());
        }

        return repo.save(existing);
    }

    public boolean isCoderAssigned(FileRecord file, List<WorkUnit> workUnits) {
        if (workUnits != null && !workUnits.isEmpty()) {
            for (WorkUnit wu : workUnits) {
                // 1. Check assigned_to JSON field
                String assignedTo = wu.getAssignedTo();
                if (assignedTo != null && !assignedTo.trim().isEmpty() && !assignedTo.trim().equals("[]")) {
                    return true;
                }
                // 2. Check work unit status
                if (wu.getStatus() != null && wu.getStatus() != WorkUnitStatus.UNASSIGNED) {
                    return true;
                }
            }
        }

        // 3. Check if any CodingResult has a coder assigned or submitted codes
        if (file.getId() != null) {
            List<CodingResult> codingResults = codingResultRepo.findByFileIdIn(List.of(file.getId()));
            for (CodingResult cr : codingResults) {
                if (cr.getCoder() != null) {
                    return true;
                }
                if (cr.getSubmittedIcdCode() != null && !cr.getSubmittedIcdCode().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Transactional
    public DeleteFilesResponse deleteFiles(List<Long> fileIds) {
        List<FileDeleteDetailDto> details = new ArrayList<>();
        int deletedCount = 0;
        int inactivatedCount = 0;

        if (fileIds == null || fileIds.isEmpty()) {
            return DeleteFilesResponse.builder()
                    .totalRequested(0)
                    .deletedCount(0)
                    .inactivatedCount(0)
                    .details(details)
                    .build();
        }

        for (Long id : fileIds) {
            Optional<FileRecord> fileOpt = repo.findById(id);
            if (fileOpt.isEmpty()) {
                details.add(FileDeleteDetailDto.builder()
                        .fileId(id)
                        .action("NOT_FOUND")
                        .message("File not found with id: " + id)
                        .build());
                continue;
            }

            FileRecord file = fileOpt.get();
            List<WorkUnit> workUnits = workUnitRepo.findByFile_Id(file.getId());
            boolean coderAssigned = isCoderAssigned(file, workUnits);

            if (coderAssigned) {
                // Coder is assigned: mark as INACTIVE, do not delete from DB or S3
                file.setStatus(Status.INACTIVE);
                repo.save(file);
                inactivatedCount++;
                details.add(FileDeleteDetailDto.builder()
                        .fileId(id)
                        .fileName(file.getFileName())
                        .action("MARKED_INACTIVE")
                        .message("Coder is assigned to this file. Status updated to INACTIVE.")
                        .build());
            } else {
                // No coder assigned: delete from S3, then delete from DB
                if (file.getS3Path() != null && !file.getS3Path().isBlank()) {
                    s3StorageService.deleteFile(file.getS3Path());
                }

                for (WorkUnit wu : workUnits) {
                    codingResultRepo.deleteByWorkUnit_Id(wu.getId());
                }
                codingResultRepo.deleteByFile_Id(file.getId());
                auditorResultRepo.deleteByFileId(file.getId());
                workUnitRepo.deleteByFile_Id(file.getId());
                patientRepo.deleteByFile_Id(file.getId());
                if (file.getS3Path() != null && !file.getS3Path().isBlank()) {
                    fileProcessingStatusRepo.deleteByS3Path(file.getS3Path());
                }
                repo.delete(file);

                deletedCount++;
                details.add(FileDeleteDetailDto.builder()
                        .fileId(id)
                        .fileName(file.getFileName())
                        .action("DELETED_PERMANENTLY")
                        .message("No coder assigned. File permanently deleted from database and S3.")
                        .build());
            }
        }

        return DeleteFilesResponse.builder()
                .totalRequested(fileIds.size())
                .deletedCount(deletedCount)
                .inactivatedCount(inactivatedCount)
                .details(details)
                .build();
    }

    @Transactional
    public FileDeleteDetailDto delete(Long id) {
        DeleteFilesResponse response = deleteFiles(List.of(id));
        if (response.getDetails().isEmpty()) {
            throw new ResourceNotFoundException("File not found with id: " + id);
        }
        FileDeleteDetailDto detail = response.getDetails().get(0);
        if ("NOT_FOUND".equals(detail.getAction())) {
            throw new ResourceNotFoundException(detail.getMessage());
        }
        return detail;
    }
}
