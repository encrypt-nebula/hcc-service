package com.example.hcc.service;

import com.example.hcc.entity.CodingResult;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CodingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public CodingResult update(Long id, CodingResult codingResult) {
        codingResult.setId(id);
        return repo.save(codingResult);
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
}
