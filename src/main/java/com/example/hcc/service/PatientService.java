package com.example.hcc.service;

import com.example.hcc.entity.Patient;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository repo;

    public Patient create(Patient patient) {
        return repo.save(patient);
    }

    public List<Patient> getAll() {
        return repo.findAll();
    }

    public Patient get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    public Patient update(Long id, Patient incoming) {

        Patient existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (incoming.getProject() != null) {
            existing.setProject(incoming.getProject());
        }

        if (incoming.getFile() != null) {
            existing.setFile(incoming.getFile());
        }

        if (incoming.getFirstName() != null) {
            existing.setFirstName(incoming.getFirstName());
        }

        if (incoming.getLastName() != null) {
            existing.setLastName(incoming.getLastName());
        }

        if (incoming.getDob() != null) {
            existing.setDob(incoming.getDob());
        }

        if (incoming.getDateOfService() != null) {
            existing.setDateOfService(incoming.getDateOfService());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Patient getByFile(Long fileId) {
        return repo.findByFileId(fileId);
    }
}
