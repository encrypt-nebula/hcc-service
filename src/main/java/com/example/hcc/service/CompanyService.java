package com.example.hcc.service;

import com.example.hcc.cognito.CognitoService;
import com.example.hcc.entity.Company;
import com.example.hcc.entity.User;
import com.example.hcc.enums.Role;
import com.example.hcc.enums.Status;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CompanyRepository;
import com.example.hcc.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repo;
    private final AuditorAware<User> auditorAware;
    private final UserRepository userRepository;
    private final CognitoService cognitoService;

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

    @Transactional
    public Company update(Long id, Company incoming) {
        Company existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }

        if (incoming.getAddress() != null) {
            existing.setAddress(incoming.getAddress());
        }

        if (incoming.getStatus() != null) {
            boolean isDisabling = existing.getStatus() != Status.INACTIVE && incoming.getStatus() == Status.INACTIVE;
            existing.setStatus(incoming.getStatus());
            if (isDisabling) {
                disableCompanyUsers(id);
            }
        }

        return repo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Company existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        existing.setStatus(Status.INACTIVE);
        repo.save(existing);
        disableCompanyUsers(id);
    }

    @Transactional
    public int bulkStatusUpdate(List<Long> companyIds, Status status) {
        int updated = repo.bulkStatusUpdate(companyIds, status.name());
        if (status == Status.INACTIVE && companyIds != null) {
            for (Long companyId : companyIds) {
                disableCompanyUsers(companyId);
            }
        }
        return updated;
    }

    public List<Company> getAllActiveCompanies() {
        return repo.findAllByStatus(Status.ACTIVE);
    }

    private void disableCompanyUsers(Long companyId) {
        List<User> users = userRepository.findByCompanyIdWithCompany(companyId);
        if (users == null || users.isEmpty()) {
            return;
        }

        for (User user : users) {
            user.setStatus(Status.INACTIVE);
            userRepository.save(user);

            String cognitoId = user.getCognitoId();
            String email = user.getEmail();

            if (cognitoId != null && !cognitoId.isBlank()) {
                try {
                    cognitoService.adminDisableUser(cognitoId);
                } catch (Exception e) {
                    log.error("Failed to disable user with cognitoId {} in AWS Cognito: {}", cognitoId, e.getMessage());
                    if (email != null && !email.isBlank()) {
                        try {
                            cognitoService.adminDisableUser(email);
                        } catch (Exception ex) {
                            log.error("Failed to disable user with email {} in AWS Cognito: {}", email, ex.getMessage());
                        }
                    }
                }
            } else if (email != null && !email.isBlank()) {
                try {
                    cognitoService.adminDisableUser(email);
                } catch (Exception e) {
                    log.error("Failed to disable user with email {} in AWS Cognito: {}", email, e.getMessage());
                }
            }
        }
    }
}
