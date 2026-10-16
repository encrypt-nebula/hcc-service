package com.example.hcc.service;

import com.example.hcc.dto.*;
import com.example.hcc.entity.MeatKeyword;
import com.example.hcc.entity.User;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.MeatKeywordRepository;
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
    private final MeatKeywordRepository meatKeywordRepository;
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

        if (incoming.getMonitor() != null) {
            existing.setMonitor(incoming.getMonitor());
        }
        if (incoming.getEvaluate() != null) {
            existing.setEvaluate(incoming.getEvaluate());
        }
        if (incoming.getAssessOrAddress() != null) {
            existing.setAssessOrAddress(incoming.getAssessOrAddress());
        }
        if (incoming.getTreat() != null) {
            existing.setTreat(incoming.getTreat());
        }
        if (incoming.getFeedback() != null) {
            existing.setFeedback(incoming.getFeedback());
        }
        if (incoming.getCoderFeedback() != null) {
            existing.setCoderFeedback(incoming.getCoderFeedback());
        }
        if (incoming.getAuditorMessage() != null) {
            existing.setAuditorMessage(incoming.getAuditorMessage());
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

        List<Long> workUnitIds = workUnits.stream().map(WorkUnit::getId).filter(Objects::nonNull).toList();
        Map<Long, Map<String, List<String>>> meatKeywordsByWorkUnit = new HashMap<>();
        if (!workUnitIds.isEmpty()) {
            List<MeatKeyword> allKeywords = meatKeywordRepository.findByWorkUnit_IdIn(workUnitIds);
            meatKeywordsByWorkUnit = allKeywords.stream()
                    .filter(k -> k.getWorkUnit() != null && k.getWorkUnit().getId() != null)
                    .collect(Collectors.groupingBy(
                            k -> k.getWorkUnit().getId(),
                            Collectors.groupingBy(
                                    MeatKeyword::getCategory,
                                    Collectors.mapping(MeatKeyword::getKeyword, Collectors.toList())
                            )
                    ));
        }

        final Map<Long, Map<String, List<String>>> finalMeatMap = meatKeywordsByWorkUnit;

        // Group by fileId
        Map<Long, List<WorkUnit>> groupedByFile = workUnits.stream()
                .collect(Collectors.groupingBy(w -> w.getFile().getId()));

        return groupedByFile.entrySet().stream()
                .map(entry -> {
                    List<WorkUnit> units = entry.getValue();
                    if (units.isEmpty()) return null;

                    WorkUnit base = units.get(0);

                    // Aggregate file-level MEAT keywords
                    Map<String, List<String>> fileMeat = new HashMap<>();

                    List<WorkUnitMergeDTO> simpleUnits = units.stream()
                            .map(w -> {
                                Map<String, List<String>> unitMeat = finalMeatMap.getOrDefault(w.getId(), Collections.emptyMap());
                                unitMeat.forEach((cat, kws) -> {
                                    fileMeat.computeIfAbsent(cat, k -> new ArrayList<>()).addAll(kws);
                                });

                                return WorkUnitMergeDTO.builder()
                                        .id(w.getId())
                                        .dos(w.getDateOfService())
                                        .monitor(w.getMonitor())
                                        .evaluate(w.getEvaluate())
                                        .assessOrAddress(w.getAssessOrAddress())
                                        .treat(w.getTreat())
                                        .meatKeywords(unitMeat)
                                        .build();
                            })
                            .toList();

                    // Deduplicate file-level keywords
                    fileMeat.replaceAll((cat, list) -> list.stream().distinct().toList());

                    return FileWorkUnitsDTO.builder()
                            .fileRecord(base.getFile())
                            .type(base.getType())
                            .status(base.getStatus())
                            .assignedTo(base.getAssignedTo())
                            .pageStart(base.getPageStart())
                            .pageEnd(base.getPageEnd())
                            .coderPageRange(base.getCoderPageRange())
                            .monitor(base.getMonitor())
                            .evaluate(base.getEvaluate())
                            .assessOrAddress(base.getAssessOrAddress())
                            .treat(base.getTreat())
                            .feedback(base.getFeedback())
                            .coderFeedback(base.getCoderFeedback())
                            .auditorMessage(base.getAuditorMessage())
                            .createdAt(base.getCreatedAt())
                            .project(base.getProject())
                            .patient(base.getPatient())
                            .meatKeywords(fileMeat)
                            .workUnits(simpleUnits)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<MeatKeyword> getMeatKeywords(Long workUnitId) {
        return meatKeywordRepository.findByWorkUnitId(workUnitId);
    }

    public MeatSummaryDTO getMeatSummary(Long workUnitId) {
        WorkUnit wu = get(workUnitId);
        List<MeatKeyword> keywords = meatKeywordRepository.findByWorkUnitId(workUnitId);
        Map<String, List<String>> categories = keywords.stream()
                .collect(Collectors.groupingBy(
                        MeatKeyword::getCategory,
                        Collectors.mapping(MeatKeyword::getKeyword, Collectors.toList())
                ));

        return MeatSummaryDTO.builder()
                .workUnitId(workUnitId)
                .monitor(wu.getMonitor())
                .evaluate(wu.getEvaluate())
                .assessOrAddress(wu.getAssessOrAddress())
                .treat(wu.getTreat())
                .categories(categories)
                .build();
    }
}
