package com.example.hcc.service;

import com.example.hcc.entity.FileRecord;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository repo;

    public FileRecord create(FileRecord fileRecord) {
        return repo.save(fileRecord);
    }

    public List<FileRecord> getAll() {
        return repo.findAll();
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

        if (incoming.getSignature() != null) {
            existing.setSignature(incoming.getSignature());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
