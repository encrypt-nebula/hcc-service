package com.example.hcc.controller;

import com.example.hcc.dto.FileCodingResultsDTO;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.service.CodingResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coding-results")
@RequiredArgsConstructor
public class CodingResultController {

    private final CodingResultService service;

    @PostMapping
    public CodingResult create(@RequestBody CodingResult codingResult) {
        return service.create(codingResult);
    }

    @GetMapping
    public List<CodingResult> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CodingResult get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public CodingResult update(@PathVariable Long id, @RequestBody CodingResult codingResult) {
        return service.update(id, codingResult);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/raf-scores")
    public Map<Long, Double> getRafScores() {
        return service.getRafScores();
    }

    @GetMapping("/assigned-to/{coderId}")
    public List<FileCodingResultsDTO> getByAssignedTo(@PathVariable Long coderId) {
        return service.getMergedCodingResultsByCoder(coderId);
    }
}

