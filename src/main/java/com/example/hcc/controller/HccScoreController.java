package com.example.hcc.controller;

import com.example.hcc.entity.HccScore;
import com.example.hcc.service.HccScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.hcc.dto.HccScoreUpdateRequest;
import com.example.hcc.entity.CodingResult;

@RestController
@RequestMapping("/hcc-scores")
@RequiredArgsConstructor
public class HccScoreController {

    private final HccScoreService service;

    @PostMapping
    public CodingResult create(@RequestBody HccScoreUpdateRequest request) {
        return service.updateCodingResultWithScores(request);
    }

    @GetMapping
    public List<HccScore> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public HccScore getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}")
    public HccScore update(@PathVariable Long id,
            @RequestBody HccScore score) {
        return service.update(id, score);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
