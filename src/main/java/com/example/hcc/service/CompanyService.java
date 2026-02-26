package com.example.hcc.service;

import com.example.hcc.entity.Company;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repo;

    public Company create(Company company) {
        return repo.save(company);
    }

    public List<Company> getAll() {
        return repo.findAll();
    }

    public Company get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    public Company update(Long id, Company company) {
        company.setId(id);
        return repo.save(company);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
