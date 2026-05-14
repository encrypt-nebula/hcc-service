package com.example.hcc.service;

import com.example.hcc.dto.ComparisonResponse;
import com.example.hcc.entity.AuditorResult;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.IcdEntry;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.AuditorResultRepository;
import com.example.hcc.repository.CodingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditorResultService {

    private final AuditorResultRepository repository;
    private final CodingResultRepository codingResultRepository;

    public AuditorResult saveOrUpdate(AuditorResult incoming) {
        if (incoming.getWorkUnit() != null && incoming.getWorkUnit().getId() != null) {
            return repository.findByWorkUnitId(incoming.getWorkUnit().getId())
                    .map(existing -> {
                        if (incoming.getSubmittedIcdCode() != null) {
                            existing.setSubmittedIcdCode(incoming.getSubmittedIcdCode());
                        }
                        if (incoming.getManualIcdCode() != null) {
                            existing.setManualIcdCode(incoming.getManualIcdCode());
                        }
                        if (incoming.getHccScore() != null) {
                            existing.setHccScore(incoming.getHccScore());
                        }
                        return repository.save(existing);
                    })
                    .orElseGet(() -> repository.save(incoming));
        }
        return repository.save(incoming);
    }

    public AuditorResult getByWorkUnitId(Long workUnitId) {
        return repository.findByWorkUnitId(workUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor result not found for work unit: " + workUnitId));
    }

    public ComparisonResponse compare(Long workUnitId) {
        CodingResult coderResult = codingResultRepository.findByWorkUnitId(workUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Coding result not found for work unit: " + workUnitId));
        
        AuditorResult auditorResult = repository.findByWorkUnitId(workUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor result not found for work unit: " + workUnitId));

        List<IcdEntry> coderCodes = coderResult.getSubmittedIcdCode() != null ? coderResult.getSubmittedIcdCode() : new ArrayList<>();
        List<IcdEntry> auditorCodes = auditorResult.getSubmittedIcdCode() != null ? auditorResult.getSubmittedIcdCode() : new ArrayList<>();

        Set<String> coderCodeSet = coderCodes.stream().map(IcdEntry::getIcdCode).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> auditorCodeSet = auditorCodes.stream().map(IcdEntry::getIcdCode).filter(Objects::nonNull).collect(Collectors.toSet());

        List<IcdEntry> common = coderCodes.stream()
                .filter(c -> auditorCodeSet.contains(c.getIcdCode()))
                .toList();

        List<IcdEntry> missingInAuditor = coderCodes.stream()
                .filter(c -> !auditorCodeSet.contains(c.getIcdCode()))
                .toList();

        List<IcdEntry> extraInAuditor = auditorCodes.stream()
                .filter(a -> !coderCodeSet.contains(a.getIcdCode()))
                .toList();

        return ComparisonResponse.builder()
                .coderCodes(coderCodes)
                .auditorCodes(auditorCodes)
                .commonCodes(common)
                .missingInAuditor(missingInAuditor)
                .extraInAuditor(extraInAuditor)
                .build();
    }
}
