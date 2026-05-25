package com.example.hcc.service;

import com.example.hcc.dto.ComparisonResponse;
import com.example.hcc.dto.FileAuditorResultsDTO;
import com.example.hcc.dto.CodingResultMergeDTO;
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
import java.util.Map;
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

        Set<String> coderCodeSet = coderCodes.stream().map(IcdEntry::getCode).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> auditorCodeSet = auditorCodes.stream().map(IcdEntry::getCode).filter(Objects::nonNull).collect(Collectors.toSet());

        List<IcdEntry> common = coderCodes.stream()
                .filter(c -> auditorCodeSet.contains(c.getCode()))
                .toList();

        List<IcdEntry> missingInAuditor = coderCodes.stream()
                .filter(c -> !auditorCodeSet.contains(c.getCode()))
                .toList();

        List<IcdEntry> extraInAuditor = auditorCodes.stream()
                .filter(a -> !coderCodeSet.contains(a.getCode()))
                .toList();

        return ComparisonResponse.builder()
                .coderCodes(coderCodes)
                .auditorCodes(auditorCodes)
                .commonCodes(common)
                .missingInAuditor(missingInAuditor)
                .extraInAuditor(extraInAuditor)
                .coderSubmission(coderResult)
                .auditorSubmission(auditorResult)
                .build();
    }

    public List<FileAuditorResultsDTO> getMergedAuditorResultsByAuditor(Long auditorId) {
        List<AuditorResult> results = repository.findByAuditor_Id(auditorId);
        return getMergedData(results);
    }

    private List<FileAuditorResultsDTO> getMergedData(List<AuditorResult> results) {
        Map<Long, List<AuditorResult>> groupedByFile = results.stream()
                .collect(Collectors.groupingBy(cr -> cr.getFile().getId()));

        return groupedByFile.entrySet().stream()
                .map(entry -> {
                    List<AuditorResult> crList = entry.getValue();
                    if (crList.isEmpty()) return null;

                    AuditorResult base = crList.get(0); 

                    List<CodingResultMergeDTO> mergedList = crList.stream()
                            .map(cr -> CodingResultMergeDTO.builder()
                                    .id(cr.getId())
                                    .dos(cr.getDos())
                                    .manualIcdCode(cr.getManualIcdCode())
                                    .aiIcdCode(cr.getAiIcdCode())
                                    .extractedIcdCode(cr.getExtractedIcdCode())
                                    .submittedIcdCode(cr.getSubmittedIcdCode())
                                    .build())
                            .toList();

                    return FileAuditorResultsDTO.builder()
                            .fileRecord(base.getFile())
                            .workUnit(base.getWorkUnit())
                            .auditor(base.getAuditor())
                            .createdAt(base.getCreatedAt())
                            .hccScore(base.getHccScore())
                            .auditorResults(mergedList)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
