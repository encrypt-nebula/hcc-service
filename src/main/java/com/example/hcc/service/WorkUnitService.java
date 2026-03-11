package com.example.hcc.service;

import com.example.hcc.dto.*;
import com.example.hcc.entity.User;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.UserRepository;
import com.example.hcc.repository.WorkUnitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkUnitService {

    private final WorkUnitRepository repo;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    public WorkUnit create(WorkUnit workUnit) {
        workUnit.setStatus(WorkUnitStatus.UNASSIGNED);
        return repo.save(workUnit);
    }

    public List<WorkUnit> getAll() {
        return repo.findAll();
    }

    public WorkUnit get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("WorkUnit not found"));
    }

    public WorkUnit update(Long id, WorkUnit incoming) {

        WorkUnit existing = get(id);

        if (incoming.getProject() != null) {
            existing.setProject(incoming.getProject());
        }

        if (incoming.getFile() != null) {
            existing.setFile(incoming.getFile());
        }

        if (incoming.getPatient() != null) {
            existing.setPatient(incoming.getPatient());
        }

        if (incoming.getType() != null) {
            existing.setType(incoming.getType());
        }

        if (incoming.getPageStart() != null) {
            existing.setPageStart(incoming.getPageStart());
        }

        if (incoming.getPageEnd() != null) {
            existing.setPageEnd(incoming.getPageEnd());
        }

        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }

        if (incoming.getAssignedTo() != null) {
            existing.setAssignedTo(incoming.getAssignedTo());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public String assignToCoder(Long coderId, List<PageRangeRequest> requests) {

        User coder = userRepository.findById(coderId)
                .orElseThrow(() -> new RuntimeException("Coder not found"));

        int totalUpdated = 0;
        for (PageRangeRequest req : requests) {
            List<WorkUnit> workUnits = repo.findByFile_Id(req.getFileId());
            for (WorkUnit wu : workUnits) {
                List<UserResponse> assignedCoders;
                try {
                    if (wu.getAssignedTo() == null || wu.getAssignedTo().isBlank()) {
                        assignedCoders = new ArrayList<>();
                    } else {
                        System.out.println("jygfgfd");
                        assignedCoders = objectMapper.readValue(
                                wu.getAssignedTo(),
                                new TypeReference<List<UserResponse>>() {}
                        );
                        System.out.println("9827392729010");
                    }
                    System.out.println(assignedCoders);

                    boolean coderExists = assignedCoders.stream()
                            .anyMatch(u -> u.getId().equals(coderId));
                    if (!coderExists) {
                        UserResponse userResponse = UserResponse.builder()
                                .id(coder.getId())
                                .name(coder.getName())
                                .email(coder.getEmail())
                                .build();

                        assignedCoders.add(userResponse);
                        wu.setAssignedTo(objectMapper.writeValueAsString(assignedCoders));
                    }
                } catch (Exception e) {
                    System.out.println(e);
                    throw new RuntimeException("Error updating assignedTo JSON", e);
                }
                wu.setStatus(WorkUnitStatus.ASSIGNED);
                List<Map<String, Object>> coderRanges;
                try {
                    if (wu.getCoderPageRange() == null) {
                        coderRanges = new ArrayList<>();
                    } else {
                        coderRanges = objectMapper.readValue(
                                wu.getCoderPageRange(),
                                new TypeReference<>() {
                                }
                        );
                    }
                    boolean coderExists = false;
                    for (Map<String, Object> entry : coderRanges) {
                        Long existingCoderId =
                                ((Number) entry.get("coderId")).longValue();

                        if (existingCoderId.equals(coderId)) {
                            entry.put("pageRange", req.getPageRange());
                            coderExists = true;
                            break;
                        }
                    }
                    if (!coderExists) {
                        Map<String, Object> newEntry = new HashMap<>();
                        newEntry.put("coderId", coderId);
                        newEntry.put("pageRange", req.getPageRange());
                        coderRanges.add(newEntry);
                    }
                    wu.setCoderPageRange(
                            objectMapper.writeValueAsString(coderRanges)
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Error updating coderPageRange JSON", e);
                }

            }
            repo.saveAll(workUnits);
            totalUpdated += workUnits.size();
        }
        return "Coder assigned successfully (" + totalUpdated + " work units)";
    }


    // Mark work as completed
    public void completeWorkUnit(Long workUnitId) {
        WorkUnit wu = repo.findById(workUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkUnit not found"));

        wu.setStatus(WorkUnitStatus.COMPLETED);
        repo.save(wu);
    }

    public List<WorkUnit> getByProjectId(Long projectId) {
        return repo.findByProject_Id(projectId);
    }

    public List<FileWorkUnitsDTO> getMergedWorkUnitsByProject(Long projectId) {

        List<WorkUnit> workUnits = repo.findByProject_Id(projectId);

        // Group by fileId
        Map<Long, List<WorkUnit>> groupedByFile = workUnits.stream()
                .collect(Collectors.groupingBy(w -> w.getFile().getId()));

        return groupedByFile.entrySet().stream()
                .map(entry -> {
                    List<WorkUnit> units = entry.getValue();
                    if (units.isEmpty()) return null;

                    WorkUnit base = units.get(0);

                    List<WorkUnitMergeDTO> simpleUnits = units.stream()
                            .map(w -> WorkUnitMergeDTO.builder()
                                    .id(w.getId())
                                    .dos(w.getDateOfService())
                                    .build())
                            .toList();

                    return FileWorkUnitsDTO.builder()
                            .fileId(base.getFile().getId())
                            .type(base.getType())
                            .status(base.getStatus())
                            .assignedTo(base.getAssignedTo())
                            .monitor(base.getMonitor())
                            .evaluate(base.getEvaluate())
                            .assessOrAddress(base.getAssessOrAddress())
                            .treat(base.getTreat())
                            .pageStart(base.getPageStart())
                            .pageEnd(base.getPageEnd())
                            .meatValidation(base.getMeatValidation())
                            .coderPageRange(base.getCoderPageRange())
                            .createdAt(base.getCreatedAt())
                            .project(base.getProject())
                            .patient(base.getPatient())
                            .workUnits(simpleUnits)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
