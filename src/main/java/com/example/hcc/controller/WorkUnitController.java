package com.example.hcc.controller;

import com.example.hcc.dto.AssignWorkUnitsRequest;
import com.example.hcc.dto.FileWorkUnitsDTO;
import com.example.hcc.dto.WorkUnitResponse;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.service.WorkUnitService;
import lombok.RequiredArgsConstructor;
import org.hibernate.jdbc.Work;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/work-units")
@RequiredArgsConstructor
public class WorkUnitController {

    private final WorkUnitService service;

    @PostMapping
    public WorkUnit create(@RequestBody WorkUnit workUnit) {
        return service.create(workUnit);
    }

    @GetMapping
    public List<WorkUnit> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public WorkUnit get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public WorkUnit update(@PathVariable Long id, @RequestBody WorkUnit workUnit) {
        return service.update(id, workUnit);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // 1️⃣ TL assigns work to coder
    @PostMapping("/assign")
    public ResponseEntity<String> assignWork(
            @RequestParam Long coderId,
            @RequestBody AssignWorkUnitsRequest request) {

        String message = service.assignToCoder(coderId, request.getPageRangeRequest());
        return ResponseEntity.ok(message);
    }

    @GetMapping("/project/{projectId}")
    public List<FileWorkUnitsDTO> getByProject(@PathVariable Long projectId) {
        return service.getMergedWorkUnitsByProject(projectId);
    }
}

