package com.example.hcc.dto;

import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.Patient;
import com.example.hcc.entity.Project;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FileWorkUnitsDTO {
    private FileRecord fileRecord;
    private WorkUnitType type;
    private WorkUnitStatus status;
    private String assignedTo;
    private String pageStart;
    private String pageEnd;
    private String coderPageRange;
    private LocalDateTime createdAt;
    private Project project;
    private Patient patient;
    private List<WorkUnitMergeDTO> workUnits;
}