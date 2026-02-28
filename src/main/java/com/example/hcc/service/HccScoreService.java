package com.example.hcc.service;

import com.example.hcc.entity.HccScore;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.HccScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HccScoreService {

    private final HccScoreRepository repository;

    public HccScore create(HccScore score) {
        return repository.save(score);
    }

    public List<HccScore> getAll() {
        return repository.findAll();
    }

    public HccScore getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HCC Score not found"));
    }

    public HccScore update(Long id, HccScore incoming) {

        HccScore existing = getById(id);

        if (incoming.getIcdCode() != null) {
            existing.setIcdCode(incoming.getIcdCode());
        }

        if (incoming.getHccScore() != null) {
            existing.setHccScore(incoming.getHccScore());
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

