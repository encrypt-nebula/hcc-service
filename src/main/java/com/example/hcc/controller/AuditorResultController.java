package com.example.hcc.controller;

import com.example.hcc.dto.ComparisonResponse;
import com.example.hcc.entity.AuditorResult;
import com.example.hcc.service.AuditorResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auditor-results")
@RequiredArgsConstructor
public class AuditorResultController {

    private final AuditorResultService service;

    @PostMapping
    public AuditorResult saveOrUpdate(@RequestBody AuditorResult result) {
        return service.saveOrUpdate(result);
    }

    @GetMapping("/work-unit/{workUnitId}")
    public AuditorResult getByWorkUnitId(@PathVariable Long workUnitId) {
        return service.getByWorkUnitId(workUnitId);
    }

    @GetMapping("/compare/{workUnitId}")
    public ComparisonResponse compare(@PathVariable Long workUnitId) {
        return service.compare(workUnitId);
    }
}
