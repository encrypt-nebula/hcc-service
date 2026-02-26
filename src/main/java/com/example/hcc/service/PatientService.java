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

    public Patient update(Long id, Patient patient) {
        patient.setId(id);
        return repo.save(patient);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Patient getByFile(Long fileId) {
        return repo.findByFileId(fileId);
    }
}
