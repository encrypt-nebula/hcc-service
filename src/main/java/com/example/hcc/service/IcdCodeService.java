package com.example.hcc.service;

import com.example.hcc.entity.IcdCode;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.IcdCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public IcdCode update(Long id, IcdCode code) {
        IcdCode existing = getById(id);
        existing.setIcdCode(code.getIcdCode());
        existing.setDescription(code.getDescription());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

