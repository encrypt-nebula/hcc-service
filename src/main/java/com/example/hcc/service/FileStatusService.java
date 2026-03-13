package com.example.hcc.service;

import com.example.hcc.dto.FileStatusUpdateDto;
import com.example.hcc.entity.FileProcessingStatus;
import com.example.hcc.enums.FileProcessingStatusType;
import com.example.hcc.repository.FileProcessingStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileStatusService {

    private final FileProcessingStatusRepository repository;

    @Transactional
    public void updateOrCreateStatus(FileStatusUpdateDto dto) {
        FileProcessingStatus status = repository.findByS3Path(dto.getS3Path())
                .orElse(new FileProcessingStatus());

        status.setS3Path(dto.getS3Path());
        status.setStatus(FileProcessingStatusType.valueOf(dto.getStatus().toUpperCase()));
        status.setErrorMessage(dto.getErrorMessage());
        status.setTotalPages(dto.getTotalPages());

        if (dto.getFileName() != null) {
            status.setFileName(dto.getFileName());
        }
        if (dto.getProjectId() != null) {
            status.setProjectId(dto.getProjectId());
        }
        if (dto.getProjectType() != null) {
            status.setProjectType(dto.getProjectType());
        }

        repository.save(status);
    }

    @Transactional(readOnly = true)
    public java.util.List<FileProcessingStatus> getStatuses(Integer projectId) {
        if (projectId != null) {
            return repository.findByProjectId(projectId);
        }
        return repository.findAll();
    }
}
