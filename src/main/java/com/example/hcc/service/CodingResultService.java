package com.example.hcc.service;

import com.example.hcc.dto.CodingResultMergeDTO;
import com.example.hcc.dto.FileCodingResultsDTO;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.AuditorResult;
import com.example.hcc.entity.MeatKeyword;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CodingResultRepository;
import com.example.hcc.repository.AuditorResultRepository;
import com.example.hcc.repository.MeatKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CodingResultService {
    private final CodingResultRepository repo;
    private final AuditorResultRepository auditorResultRepo;
    private final MeatKeywordRepository meatKeywordRepository;

    public CodingResult create(CodingResult codingResult) {
        return repo.save(codingResult);
    }

    public List<CodingResult> getAll() {
        return repo.findAll();
    }

    public CodingResult get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("CodingResult not found"));
    }

    public CodingResult update(Long id, CodingResult incoming) {

        CodingResult existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodingResult not found"));

        if (incoming.getWorkUnit() != null) {
            existing.setWorkUnit(incoming.getWorkUnit());
        }

        if (incoming.getExtractedIcdCode() != null) {
            existing.setExtractedIcdCode(incoming.getExtractedIcdCode());
        }

        if (incoming.getManualIcdCode() != null) {
            existing.setManualIcdCode(incoming.getManualIcdCode());
        }

        if (incoming.getAiIcdCode() != null) {
            existing.setAiIcdCode(incoming.getAiIcdCode());
        }

        if (incoming.getHccScore() != null) {
            existing.setHccScore(incoming.getHccScore());
        }

        if (incoming.getSubmittedIcdCode() != null) {
            existing.setSubmittedIcdCode(incoming.getSubmittedIcdCode());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Map<Long, Double> getRafScores() {
        List<Object[]> results = repo.getRafScorePerWorkUnit();
        Map<Long, Double> rafScores = new HashMap<>();

        for (Object[] row : results) {
            Long workUnitId = ((Number) row[0]).longValue();
            BigDecimal score = (BigDecimal) row[1];  // ✅ cast to BigDecimal
            rafScores.put(workUnitId, score.doubleValue());  // ✅ convert to double
        }
        return rafScores;

    }

    public List<CodingResult> getByAssignedTo(Long coderId) {
        return repo.findByAssignedToCoder(coderId);
    }

    public List<FileCodingResultsDTO> getMergedCodingResultsByCoder(Long coderId) {

        List<CodingResult> results = repo.findByAssignedToCoder(coderId);
        return getMergedData(results);
    }

    public List<FileCodingResultsDTO> getAllMergedCodingResult() {

        List<CodingResult> results = repo.findAll();
        return getMergedData(results);
    }

    private List<FileCodingResultsDTO> getMergedData(List<CodingResult> results) {
        Map<Long, List<CodingResult>> groupedByFile = results.stream()
                .collect(Collectors.groupingBy(cr -> cr.getFile().getId()));

        List<Long> fileIds = results.stream()
                .map(cr -> cr.getFile().getId())
                .distinct()
                .toList();

        List<AuditorResult> allAuditorResults = fileIds.isEmpty() ? List.of() : auditorResultRepo.findByFileIdIn(fileIds);
        Map<Long, List<AuditorResult>> auditorResultsByFile = allAuditorResults.stream()
                .collect(Collectors.groupingBy(ar -> ar.getFile().getId()));

        List<Long> wuIds = Stream.concat(
                results.stream().map(cr -> cr.getWorkUnit() != null ? cr.getWorkUnit().getId() : null),
                allAuditorResults.stream().map(ar -> ar.getWorkUnit() != null ? ar.getWorkUnit().getId() : null)
        ).filter(Objects::nonNull).distinct().toList();

        Map<Long, Map<String, List<String>>> meatKeywordsByWorkUnit = new HashMap<>();
        if (!wuIds.isEmpty()) {
            List<MeatKeyword> keywords = meatKeywordRepository.findByWorkUnit_IdIn(wuIds);
            meatKeywordsByWorkUnit = keywords.stream()
                    .filter(k -> k.getWorkUnit() != null && k.getWorkUnit().getId() != null)
                    .collect(Collectors.groupingBy(
                            k -> k.getWorkUnit().getId(),
                            Collectors.groupingBy(
                                    MeatKeyword::getCategory,
                                    Collectors.mapping(MeatKeyword::getKeyword, Collectors.toList())
                            )
                    ));
        }

        final Map<Long, Map<String, List<String>>> finalMeatMap = meatKeywordsByWorkUnit;
        Map<Long, List<AuditorResult>> finalAuditorResultsByFile = auditorResultsByFile;

        // 3️⃣ Build merged DTOs
        return groupedByFile.entrySet().stream()
                .map(entry -> {
                    List<CodingResult> crList = entry.getValue();
                    if (crList.isEmpty()) return null;

                    CodingResult base = crList.get(0); // common fields
                    Map<String, List<String>> fileMeat = new HashMap<>();

                    List<CodingResultMergeDTO> mergedList = crList.stream()
                            .map(cr -> {
                                WorkUnit wu = cr.getWorkUnit();
                                Map<String, List<String>> unitMeat = (wu != null && wu.getId() != null)
                                        ? finalMeatMap.getOrDefault(wu.getId(), Collections.emptyMap())
                                        : Collections.emptyMap();

                                unitMeat.forEach((cat, kws) -> {
                                    fileMeat.computeIfAbsent(cat, k -> new ArrayList<>()).addAll(kws);
                                });

                                return CodingResultMergeDTO.builder()
                                        .id(cr.getId())
                                        .dos(cr.getDos())
                                        .manualIcdCode(cr.getManualIcdCode())
                                        .aiIcdCode(cr.getAiIcdCode())
                                        .extractedIcdCode(cr.getExtractedIcdCode())
                                        .submittedIcdCode(cr.getSubmittedIcdCode())
                                        .monitor(wu != null ? wu.getMonitor() : null)
                                        .evaluate(wu != null ? wu.getEvaluate() : null)
                                        .assessOrAddress(wu != null ? wu.getAssessOrAddress() : null)
                                        .treat(wu != null ? wu.getTreat() : null)
                                        .meatKeywords(unitMeat)
                                        .build();
                            })
                            .toList();

                    List<AuditorResult> arList = finalAuditorResultsByFile.getOrDefault(entry.getKey(), List.of());
                    List<CodingResultMergeDTO> mergedAuditorList = arList.stream()
                            .map(ar -> {
                                WorkUnit wu = ar.getWorkUnit();
                                Map<String, List<String>> unitMeat = (wu != null && wu.getId() != null)
                                        ? finalMeatMap.getOrDefault(wu.getId(), Collections.emptyMap())
                                        : Collections.emptyMap();

                                unitMeat.forEach((cat, kws) -> {
                                    fileMeat.computeIfAbsent(cat, k -> new ArrayList<>()).addAll(kws);
                                });

                                return CodingResultMergeDTO.builder()
                                        .id(ar.getId())
                                        .dos(ar.getDos())
                                        .manualIcdCode(ar.getManualIcdCode())
                                        .aiIcdCode(ar.getAiIcdCode())
                                        .extractedIcdCode(ar.getExtractedIcdCode())
                                        .submittedIcdCode(ar.getSubmittedIcdCode())
                                        .monitor(wu != null ? wu.getMonitor() : null)
                                        .evaluate(wu != null ? wu.getEvaluate() : null)
                                        .assessOrAddress(wu != null ? wu.getAssessOrAddress() : null)
                                        .treat(wu != null ? wu.getTreat() : null)
                                        .meatKeywords(unitMeat)
                                        .build();
                            })
                            .toList();

                    fileMeat.replaceAll((cat, list) -> list.stream().distinct().toList());

                    WorkUnit baseWu = base.getWorkUnit();
                    return FileCodingResultsDTO.builder()
                            .fileRecord(base.getFile())
                            .workUnit(baseWu)
                            .coder(base.getCoder())
                            .createdAt(base.getCreatedAt())
                            .hccScore(base.getHccScore())
                            .monitor(baseWu != null ? baseWu.getMonitor() : null)
                            .evaluate(baseWu != null ? baseWu.getEvaluate() : null)
                            .assessOrAddress(baseWu != null ? baseWu.getAssessOrAddress() : null)
                            .treat(baseWu != null ? baseWu.getTreat() : null)
                            .meatKeywords(fileMeat)
                            .codingResults(mergedList)
                            .auditorResults(mergedAuditorList)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
