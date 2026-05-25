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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public AuditorResult saveOrUpdate(@RequestBody AuditorResult result) {
        return service.saveOrUpdate(result);
    }

    @GetMapping("/work-unit/{workUnitId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AUDITOR', 'TL', 'ADMIN', 'SUPER_ADMIN')")
    public AuditorResult getByWorkUnitId(@PathVariable Long workUnitId) {
        return service.getByWorkUnitId(workUnitId);
    }

    @GetMapping("/compare/{workUnitId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TL', 'ADMIN', 'SUPER_ADMIN')")
    public ComparisonResponse compare(@PathVariable Long workUnitId) {
        return service.compare(workUnitId);
    }

    @GetMapping("/assigned-to/{auditorId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AUDITOR', 'TL', 'ADMIN', 'SUPER_ADMIN')")
    public java.util.List<com.example.hcc.dto.FileAuditorResultsDTO> getByAssignedTo(@PathVariable Long auditorId) {
        return service.getMergedAuditorResultsByAuditor(auditorId);
    }
}
