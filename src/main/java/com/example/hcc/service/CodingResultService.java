package com.example.hcc.service;

import com.example.hcc.dto.CodingResultMergeDTO;
import com.example.hcc.dto.FileCodingResultsDTO;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CodingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodingResultService {
    private final CodingResultRepository repo;

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

        // 3️⃣ Build merged DTOs
        return groupedByFile.entrySet().stream()
                .map(entry -> {
                    List<CodingResult> crList = entry.getValue();
                    if (crList.isEmpty()) return null;

                    CodingResult base = crList.get(0); // common fields

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

                    return FileCodingResultsDTO.builder()
                            .fileRecord(base.getFile())
                            .workUnit(base.getWorkUnit())
                            .coder(base.getCoder())
                            .meatValidation(base.getMeatValidation())
                            .createdAt(base.getCreatedAt())
                            .hccScore(base.getHccScore()) // if different per row, can compute average/sum
                            .codingResults(mergedList)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
