package com.example.hcc.service;

import com.example.hcc.dto.ComparisonResponse;
import com.example.hcc.dto.FileAuditorResultsDTO;
import com.example.hcc.dto.CodingResultMergeDTO;
import com.example.hcc.entity.AuditorResult;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.IcdEntry;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.User;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.repository.AuditorResultRepository;
import com.example.hcc.repository.CodingResultRepository;
import com.example.hcc.repository.FileRepository;
import com.example.hcc.repository.WorkUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private final FileRepository fileRepository;
    private final WorkUnitRepository workUnitRepository;

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
        List<FileRecord> assignedFiles = fileRepository.findByAuditor_Id(auditorId);
        List<AuditorResult> results = repository.findByAuditor_Id(auditorId);

        List<FileRecord> allFiles = new ArrayList<>(assignedFiles);
        Set<Long> assignedFileIds = assignedFiles.stream().map(FileRecord::getId).collect(Collectors.toSet());
        for (AuditorResult ar : results) {
            if (ar.getFile() != null && !assignedFileIds.contains(ar.getFile().getId())) {
                allFiles.add(ar.getFile());
                assignedFileIds.add(ar.getFile().getId());
            }
        }

        return getMergedData(allFiles, results);
    }

    private List<FileAuditorResultsDTO> getMergedData(List<FileRecord> files, List<AuditorResult> results) {
        Map<Long, List<AuditorResult>> groupedByFile = results.stream()
                .filter(cr -> cr.getFile() != null)
                .collect(Collectors.groupingBy(cr -> cr.getFile().getId()));

        List<Long> fileIds = files.stream()
                .map(FileRecord::getId)
                .toList();

        Map<Long, List<CodingResult>> codingResultsByFile = new HashMap<>();
        if (!fileIds.isEmpty()) {
            List<CodingResult> allCodingResults = codingResultRepository.findByFileIdIn(fileIds);
            codingResultsByFile = allCodingResults.stream()
                    .collect(Collectors.groupingBy(cr -> cr.getFile().getId()));
        }

        Map<Long, List<CodingResult>> finalCodingResultsByFile = codingResultsByFile;

        return files.stream()
                .<FileAuditorResultsDTO>map(file -> {
                    List<AuditorResult> crList = groupedByFile.getOrDefault(file.getId(), List.of());
                    List<CodingResult> codingList = finalCodingResultsByFile.getOrDefault(file.getId(), List.of());

                    WorkUnit workUnit = null;
                    User auditor = file.getAuditor();
                    LocalDateTime createdAt = null;
                    BigDecimal hccScore = null;

                    if (!crList.isEmpty()) {
                        AuditorResult base = crList.get(0);
                        workUnit = base.getWorkUnit();
                        if (base.getAuditor() != null) {
                            auditor = base.getAuditor();
                        }
                        createdAt = base.getCreatedAt();
                        hccScore = base.getHccScore();
                    } else if (!codingList.isEmpty()) {
                        workUnit = codingList.get(0).getWorkUnit();
                    } else {
                        List<WorkUnit> wus = workUnitRepository.findByFile_Id(file.getId());
                        if (!wus.isEmpty()) {
                            workUnit = wus.get(0);
                        }
                    }

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

                    List<CodingResultMergeDTO> mergedCodingList = codingList.stream()
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
                            .fileRecord(file)
                            .workUnit(workUnit)
                            .auditor(auditor)
                            .createdAt(createdAt)
                            .hccScore(hccScore)
                            .auditorResults(mergedList)
                            .codingResults(mergedCodingList)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
