package com.example.hcc.service;

import com.example.hcc.entity.Company;
import com.example.hcc.entity.User;
import com.example.hcc.enums.Role;
import com.example.hcc.enums.Status;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repo;
    private final AuditorAware<User> auditorAware;

    public Company create(Company company) {
        return repo.save(company);
    }

    public List<Company> getAll() {
        return repo.findAll();
    }

    public List<Company> getCompaniesByCurrentUser() {
        User user = auditorAware.getCurrentAuditor()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN) {
            return repo.findAll();
        }

        if (user.getCompany() != null) {
            return List.of(user.getCompany());
        }

        return Collections.emptyList();
    }

    public Company get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    public Company update(Long id, Company incoming) {
        Company existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }

        if (incoming.getAddress() != null) {
            existing.setAddress(incoming.getAddress());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public int bulkStatusUpdate(List<Long> projectIds, Status status) {
        return repo.bulkStatusUpdate(projectIds, status.name());
    }

    public List<Company> getAllActiveCompanies() {
        return repo.findAllByStatus(Status.ACTIVE);
    }
}
