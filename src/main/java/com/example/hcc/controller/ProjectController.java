package com.example.hcc.controller;

import com.example.hcc.dto.BulkDeactivateRequest;
import com.example.hcc.dto.ExtractDataResponse;
import com.example.hcc.dto.ProjectResponse;
import com.example.hcc.entity.Project;
import com.example.hcc.mapper.ProjectMapper;
import com.example.hcc.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @PostMapping
    public ProjectResponse create(@RequestBody Project project) {
        Project saved = service.create(project);
        return ProjectMapper.toResponse(saved);
    }

    @GetMapping
    public List<Project> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Project get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @RequestBody Project project) {
        Project update = service.update(id, project);
        return ProjectMapper.toResponse(update);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/bulk-deactivate")
    public ResponseEntity<?> bulkDeactivate(
            @Valid @RequestBody BulkDeactivateRequest request) {

        int deleted = service.bulkDeactivate(request.getIds());

        return ResponseEntity.ok(
                new ExtractDataResponse("SUCCESS",
                        deleted + " projects deactived successfully")
        );
    }
}

