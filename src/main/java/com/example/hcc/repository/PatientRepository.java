package com.example.hcc.repository;

import com.example.hcc.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Patient findByFileId(Long fileId);

    void deleteByFile_Id(Long fileId);
}
