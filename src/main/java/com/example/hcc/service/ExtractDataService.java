package com.example.hcc.service;

import com.example.hcc.dto.ExtractDataRequest;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.Patient;
import com.example.hcc.entity.Project;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.mapper.ExtractDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExtractDataService {

    private final ProjectService projectService;
    private final FileService fileService;
    private final PatientService patientService;
    private final WorkUnitService workUnitService;
    private final CodingResultService codingResultService;
    private final ExtractDataMapper mapper;

    public void process(ExtractDataRequest request) {
        System.out.println("Processing extract data for project: " + request);
        Project project = projectService.get(
                request.getProjectId()
        );

        FileRecord file = fileService.create(
                mapper.mapFileRecord(request, project)
        );

        Patient patient = patientService.create(
                mapper.mapPatient(request, project, file)
        );

        WorkUnit workUnit = workUnitService.create(
                mapper.mapWorkUnit(request, project, file, patient)
        );

        request.getDetails().forEach(detail ->
                codingResultService.create(
                        mapper.mapCodingResult(workUnit, detail)
                )
        );

    }
}