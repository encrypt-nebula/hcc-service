package com.example.hcc.controller;

import com.example.hcc.dto.AssignWorkUnitsRequest;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.service.WorkUnitService;
import lombok.RequiredArgsConstructor;
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
            @RequestBody AssignWorkUnitsRequest assignWorkUnitsRequest) {

        String message = service.assignToCoder(coderId, assignWorkUnitsRequest.getWorkUnitIds());
        return ResponseEntity.ok(message);    }

    // 2️⃣ Coder fetches assigned work
    @GetMapping("/assigned/{coderId}")
    public List<WorkUnit> getAssignedWork(@PathVariable Long coderId) {
        return service.fetchAssignedWork(coderId);
    }

    // 3️⃣ Coder picks work (ASSIGNED → IN_PROGRESS)
    @PostMapping("/pick/{coderId}")
    public List<WorkUnit> pickWork(@PathVariable Long coderId) {
        return service.pickWork(coderId);
    }
}

