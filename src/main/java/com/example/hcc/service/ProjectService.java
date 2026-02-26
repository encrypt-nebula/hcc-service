package com.example.hcc.service;

import com.example.hcc.entity.Project;
import com.example.hcc.entity.User;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.ProjectRepository;
import com.example.hcc.repository.UserRepository;
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

    public Project update(Long id, Project project) {
        project.setId(id);
        return repo.save(project);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
