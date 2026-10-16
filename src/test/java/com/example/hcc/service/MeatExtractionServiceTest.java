package com.example.hcc.service;

import com.example.hcc.dto.DataExtractionDto;
import com.example.hcc.dto.FileWorkUnitsDTO;
import com.example.hcc.dto.MeatSummaryDTO;
import com.example.hcc.entity.*;
import com.example.hcc.enums.ProjectType;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import com.example.hcc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeatExtractionServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private WorkUnitRepository workUnitRepository;
    @Mock
    private CodingResultRepository codingResultRepository;
    @Mock
    private MeatKeywordRepository meatKeywordRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataExtractionService dataExtractionService;

    private WorkUnitService workUnitService;

    @BeforeEach
    void setUp() {
        workUnitService = new WorkUnitService(workUnitRepository, userRepository, meatKeywordRepository, new ObjectMapper());
    }

    @Test
    void testProcessExtraction_SavesMeatKeywordsAndFlags() {
        Project project = new Project();
        project.setId(1L);
        project.setProjectType(ProjectType.PROSPECTIVE);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(fileRepository.findByS3Path(any())).thenReturn(Optional.empty());
        when(fileRepository.save(any())).thenAnswer(invocation -> {
            FileRecord f = invocation.getArgument(0);
            f.setId(10L);
            return f;
        });
        when(patientRepository.save(any())).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            p.setId(20L);
            return p;
        });
        when(workUnitRepository.save(any())).thenAnswer(invocation -> {
            WorkUnit wu = invocation.getArgument(0);
            if (wu.getId() == null) wu.setId(100L);
            return wu;
        });

        DataExtractionDto dto = new DataExtractionDto();
        dto.setProjectId(1L);
        dto.setFileName("Veradigm_W1_23_NPU7012747201 - 100.pdf");
        dto.setS3Path("s3://bucket/test.pdf");
        dto.setDos("2024-05-10");

        DataExtractionDto.EncounterDetailDto detail = new DataExtractionDto.EncounterDetailDto();
        detail.setDos("2024-05-10");
        detail.setMonitor(true);
        detail.setEvaluate(true);
        detail.setAssessOrAddress(true);
        detail.setTreat(true);
        detail.setMonitoringKeywords(List.of("reviewed", "follow-up"));
        detail.setEvaluationKeywords(List.of("ordered"));
        detail.setAssessmentKeywords(List.of("stable", "mild"));
        detail.setTreatmentKeywords(List.of("continue", "started"));

        dto.setDetails(List.of(detail));

        dataExtractionService.processExtraction(dto);

        // Verify WorkUnit was saved with MEAT flags
        ArgumentCaptor<WorkUnit> wuCaptor = ArgumentCaptor.forClass(WorkUnit.class);
        verify(workUnitRepository, atLeastOnce()).save(wuCaptor.capture());
        WorkUnit savedWu = wuCaptor.getValue();
        assertTrue(savedWu.getMonitor());
        assertTrue(savedWu.getEvaluate());
        assertTrue(savedWu.getAssessOrAddress());
        assertTrue(savedWu.getTreat());

        // Verify MEAT keywords were persisted
        ArgumentCaptor<MeatKeyword> kwCaptor = ArgumentCaptor.forClass(MeatKeyword.class);
        verify(meatKeywordRepository, atLeast(7)).save(kwCaptor.capture());
        List<MeatKeyword> savedKeywords = kwCaptor.getAllValues();
        assertEquals(7, savedKeywords.size());
        assertTrue(savedKeywords.stream().anyMatch(k -> k.getCategory().equals("MONITORING") && k.getKeyword().equals("reviewed")));
        assertTrue(savedKeywords.stream().anyMatch(k -> k.getCategory().equals("TREATMENT") && k.getKeyword().equals("continue")));
    }

    @Test
    void testGetMeatSummary_ReturnsCategorizedPayload() {
        WorkUnit wu = WorkUnit.builder()
                .id(100L)
                .monitor(true)
                .evaluate(false)
                .assessOrAddress(true)
                .treat(true)
                .build();

        when(workUnitRepository.findById(100L)).thenReturn(Optional.of(wu));

        List<MeatKeyword> keywords = List.of(
                MeatKeyword.builder().category("MONITORING").keyword("vitals reviewed").workUnit(wu).build(),
                MeatKeyword.builder().category("ASSESSMENT").keyword("controlled").workUnit(wu).build(),
                MeatKeyword.builder().category("TREATMENT").keyword("prescribed metformin").workUnit(wu).build()
        );
        when(meatKeywordRepository.findByWorkUnitId(100L)).thenReturn(keywords);

        MeatSummaryDTO summary = workUnitService.getMeatSummary(100L);

        assertNotNull(summary);
        assertEquals(100L, summary.getWorkUnitId());
        assertTrue(summary.getMonitor());
        assertFalse(summary.getEvaluate());
        assertTrue(summary.getAssessOrAddress());
        assertTrue(summary.getTreat());
        assertEquals(List.of("vitals reviewed"), summary.getCategories().get("MONITORING"));
        assertEquals(List.of("controlled"), summary.getCategories().get("ASSESSMENT"));
        assertEquals(List.of("prescribed metformin"), summary.getCategories().get("TREATMENT"));
    }

    @Test
    void testGetMergedWorkUnitsByProject_EmbedsMeatKeywords() {
        FileRecord file = FileRecord.builder().id(5L).fileName("record.pdf").build();
        WorkUnit wu = WorkUnit.builder()
                .id(100L)
                .file(file)
                .dateOfService(LocalDate.of(2024, 5, 10))
                .monitor(true)
                .evaluate(true)
                .assessOrAddress(false)
                .treat(true)
                .build();

        when(workUnitRepository.findByProject_Id(1L)).thenReturn(List.of(wu));

        List<MeatKeyword> keywords = List.of(
                MeatKeyword.builder().category("MONITORING").keyword("recheck").workUnit(wu).build(),
                MeatKeyword.builder().category("EVALUATION").keyword("lab ordered").workUnit(wu).build()
        );
        when(meatKeywordRepository.findByWorkUnit_IdIn(List.of(100L))).thenReturn(keywords);

        List<FileWorkUnitsDTO> result = workUnitService.getMergedWorkUnitsByProject(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        FileWorkUnitsDTO fileDto = result.get(0);
        assertTrue(fileDto.getMonitor());
        assertTrue(fileDto.getEvaluate());
        assertNotNull(fileDto.getMeatKeywords());
        assertTrue(fileDto.getMeatKeywords().containsKey("MONITORING"));
        assertEquals(List.of("recheck"), fileDto.getMeatKeywords().get("MONITORING"));

        // Nested workUnit
        assertEquals(1, fileDto.getWorkUnits().size());
        assertEquals(List.of("recheck"), fileDto.getWorkUnits().get(0).getMeatKeywords().get("MONITORING"));
    }
}
