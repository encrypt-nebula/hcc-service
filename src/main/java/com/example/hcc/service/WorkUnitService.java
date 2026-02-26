package com.example.hcc.service;

import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.WorkUnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkUnitService {
    private final WorkUnitRepository repo;

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

    public WorkUnit update(Long id, WorkUnit workUnit) {
        WorkUnit workUnitExisting = get(id);
        workUnit.setStatus(workUnitExisting.getStatus());
        workUnit.setId(id);
        return repo.save(workUnit);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    // TL assigns work units to coder
    @Transactional
    public String assignToCoder(Long coderId, List<Long> workUnitIds) {
        int updated = repo.assignWorkUnits(coderId, workUnitIds);

        if (updated == 0) {
            throw new RuntimeException("No work units assigned");
        }

        return "Coder assigned successfully (" + updated + " work units)";
    }
    // Coder fetches assigned work
    public List<WorkUnit> fetchAssignedWork(Long coderId) {
        return repo.findByAssignedToIdAndStatus(
                coderId,
                WorkUnitStatus.ASSIGNED
        );
    }

    @Transactional
    public List<WorkUnit> pickWork(Long coderId) {
        List<WorkUnit> units = repo.findByAssignedToIdAndStatus(coderId, WorkUnitStatus.ASSIGNED);
        units.forEach(w -> w.setStatus(WorkUnitStatus.IN_PROGRESS));
        return repo.saveAll(units);
    }

    // Mark work as completed
    public void completeWorkUnit(Long workUnitId) {
        WorkUnit wu = repo.findById(workUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkUnit not found"));

        wu.setStatus(WorkUnitStatus.COMPLETED);
        repo.save(wu);
    }
}
