package com.example.hcc.service;

import com.example.hcc.entity.CoderSession;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CoderSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoderSessionService {

    private final CoderSessionRepository repository;

    public CoderSession create(CoderSession session) {
        session.setStartedAt(LocalDateTime.now());
        return repository.save(session);
    }

    public List<CoderSession> getAll() {
        return repository.findAll();
    }

    public CoderSession getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CoderSession not found"));
    }

    public CoderSession update(Long id, CoderSession incoming) {

        CoderSession existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CoderSession not found"));

        if (incoming.getWorkUnitId() != null) {
            existing.setWorkUnitId(incoming.getWorkUnitId());
        }

        if (incoming.getCoderId() != null) {
            existing.setCoderId(incoming.getCoderId());
        }

        if (incoming.getStartedAt() != null) {
            existing.setStartedAt(incoming.getStartedAt());
        }

        if (incoming.getCompletedAt() != null) {
            existing.setCompletedAt(incoming.getCompletedAt());
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

