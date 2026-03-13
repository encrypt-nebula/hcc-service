package com.example.hcc.service;

import com.example.hcc.entity.IcdCode;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.IcdCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IcdCodeService {

    private final IcdCodeRepository repository;

    public IcdCode create(IcdCode code) {
        return repository.save(code);
    }

    public List<IcdCode> getAll() {
        return repository.findAll();
    }

    public IcdCode getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ICD Code not found"));
    }

    public IcdCode update(Long id, IcdCode incoming) {

        IcdCode existing = getById(id);

        if (incoming.getIcdCode() != null) {
            existing.setIcdCode(incoming.getIcdCode());
        }

        if (incoming.getHccScore() != null) {
            existing.setHccScore(incoming.getHccScore());
        }

        if (incoming.getDescription() != null) {
            existing.setDescription(incoming.getDescription());
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<IcdCode> getByIcdCode(String icdCode) {
        return repository.findByIcdCode(icdCode);
    }

    public List<IcdCode> getByIcdCodes(List<String> icdCodes) {
        return repository.findByIcdCodeIn(icdCodes);
    }

    public List<String> validateCodes(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return List.of();
        }
        return queries.stream()
                .flatMap(query -> repository.findValidCodesByQuery(query).stream())
                .distinct()
                .toList();
    }
}
