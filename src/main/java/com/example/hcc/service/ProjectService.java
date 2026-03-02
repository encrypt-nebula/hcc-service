package com.example.hcc.service;

import com.example.hcc.entity.Project;
import com.example.hcc.entity.User;
import com.example.hcc.enums.Status;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.ProjectRepository;
import com.example.hcc.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository repo;

    public Project create(Project project) {
        return repo.save(project);
    }

    public List<Project> getAll() {
        return repo.findAll();
    }

    public Project get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    public Project update(Long id, Project incoming) {
        Project existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (incoming.getProjectName() != null) {
            existing.setProjectName(incoming.getProjectName());
        }

        if (incoming.getProjectType() != null) {
            existing.setProjectType(incoming.getProjectType());
        }

        if (incoming.getCredentials() != null) {
            existing.setCredentials(incoming.getCredentials());
        }

        if (incoming.getReviewMode() != null) {
            existing.setReviewMode(incoming.getReviewMode());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public int bulkStatusUpdate(List<Long> projectIds, Status status) {
        return repo.bulkStatusUpdate(projectIds, status.name());
    }

    public List<Project> getAllActiveCompanies() {
        return repo.findAllByStatus(Status.ACTIVE);
    }
}
