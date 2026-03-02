package com.example.hcc.service;

import com.example.hcc.entity.WorkUnit;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.CodingResultRepository;
import com.example.hcc.repository.FileRepository;
import com.example.hcc.repository.PatientRepository;
import com.example.hcc.repository.WorkUnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtractDataDeleteService {

    private final CodingResultRepository codingResultRepo;
    private final WorkUnitRepository workUnitRepo;
    private final PatientRepository patientRepo;
    private final FileRepository fileRepo;

    @Transactional
    public void deleteExtractData(Long fileId) {

        fileRepo.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("File not found with id: " + fileId)
                );

        List<WorkUnit> workUnits = workUnitRepo.findByFile_Id(fileId);

        for (WorkUnit wu : workUnits) {
            codingResultRepo.deleteByWorkUnit_Id(wu.getId());
        }

        workUnitRepo.deleteByFile_Id(fileId);

        patientRepo.deleteByFile_Id(fileId);

        fileRepo.deleteById(fileId);
    }
}
