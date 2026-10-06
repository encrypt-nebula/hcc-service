package com.example.hcc.service;

import com.example.hcc.dto.DeleteFilesResponse;
import com.example.hcc.dto.FileDeleteDetailDto;
import com.example.hcc.entity.CodingResult;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.entity.WorkUnit;
import com.example.hcc.enums.Status;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepo;

    @Mock
    private WorkUnitRepository workUnitRepo;

    @Mock
    private CodingResultRepository codingResultRepo;

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private AuditorResultRepository auditorResultRepo;

    @Mock
    private FileProcessingStatusRepository fileProcessingStatusRepo;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private FileService fileService;

    private FileRecord activeFile;

    @BeforeEach
    void setUp() {
        activeFile = FileRecord.builder()
                .id(1L)
                .fileName("test.pdf")
                .s3Path("s3://test-bucket/docs/test.pdf")
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void testCreate_SetsActiveByDefaultIfNull() {
        FileRecord newFile = FileRecord.builder()
                .fileName("new.pdf")
                .status(null)
                .build();

        when(fileRepo.save(any(FileRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileRecord saved = fileService.create(newFile);
        assertEquals(Status.ACTIVE, saved.getStatus());
    }

    @Test
    void testGetAll_FiltersByStatus() {
        when(fileRepo.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(activeFile));
        when(fileRepo.findAllByStatus(Status.INACTIVE)).thenReturn(Collections.emptyList());
        when(fileRepo.findAll()).thenReturn(List.of(activeFile));

        List<FileRecord> activeList = fileService.getAll("ACTIVE");
        assertEquals(1, activeList.size());

        List<FileRecord> defaultList = fileService.getAll(null);
        assertEquals(1, defaultList.size());

        List<FileRecord> inactiveList = fileService.getAll("INACTIVE");
        assertTrue(inactiveList.isEmpty());

        List<FileRecord> allList = fileService.getAll("ALL");
        assertEquals(1, allList.size());
    }

    @Test
    void testDeleteFiles_WhenCoderIsAssigned_MarksAsInactive() {
        WorkUnit assignedWu = WorkUnit.builder()
                .id(10L)
                .file(activeFile)
                .status(WorkUnitStatus.ASSIGNED)
                .assignedTo("[{\"id\":2,\"name\":\"John\"}]")
                .build();

        when(fileRepo.findById(1L)).thenReturn(Optional.of(activeFile));
        when(workUnitRepo.findByFile_Id(1L)).thenReturn(List.of(assignedWu));
        when(fileRepo.save(any(FileRecord.class))).thenReturn(activeFile);

        DeleteFilesResponse response = fileService.deleteFiles(List.of(1L));

        assertEquals(1, response.getTotalRequested());
        assertEquals(0, response.getDeletedCount());
        assertEquals(1, response.getInactivatedCount());

        FileDeleteDetailDto detail = response.getDetails().get(0);
        assertEquals("MARKED_INACTIVE", detail.getAction());
        assertEquals(Status.INACTIVE, activeFile.getStatus());

        // Verify S3 deletion and DB delete were NOT called
        verify(s3StorageService, never()).deleteFile(anyString());
        verify(fileRepo, never()).delete(any(FileRecord.class));
        verify(fileRepo).save(activeFile);
    }

    @Test
    void testDeleteFiles_WhenNoCoderIsAssigned_DeletesFromS3AndDB() {
        WorkUnit unassignedWu = WorkUnit.builder()
                .id(10L)
                .file(activeFile)
                .status(WorkUnitStatus.UNASSIGNED)
                .assignedTo(null)
                .build();

        when(fileRepo.findById(1L)).thenReturn(Optional.of(activeFile));
        when(workUnitRepo.findByFile_Id(1L)).thenReturn(List.of(unassignedWu));
        when(codingResultRepo.findByFileIdIn(anyList())).thenReturn(Collections.emptyList());

        DeleteFilesResponse response = fileService.deleteFiles(List.of(1L));

        assertEquals(1, response.getTotalRequested());
        assertEquals(1, response.getDeletedCount());
        assertEquals(0, response.getInactivatedCount());

        FileDeleteDetailDto detail = response.getDetails().get(0);
        assertEquals("DELETED_PERMANENTLY", detail.getAction());

        // Verify S3 deletion was called
        verify(s3StorageService).deleteFile("s3://test-bucket/docs/test.pdf");

        // Verify DB cascading deletion
        verify(codingResultRepo).deleteByWorkUnit_Id(10L);
        verify(codingResultRepo).deleteByFile_Id(1L);
        verify(auditorResultRepo).deleteByFileId(1L);
        verify(workUnitRepo).deleteByFile_Id(1L);
        verify(patientRepo).deleteByFile_Id(1L);
        verify(fileProcessingStatusRepo).deleteByS3Path("s3://test-bucket/docs/test.pdf");
        verify(fileRepo).delete(activeFile);
    }

    @Test
    void testDeleteFiles_WhenFileNotFound_ReportsNotFound() {
        when(fileRepo.findById(999L)).thenReturn(Optional.empty());

        DeleteFilesResponse response = fileService.deleteFiles(List.of(999L));

        assertEquals(1, response.getTotalRequested());
        assertEquals(0, response.getDeletedCount());
        assertEquals(0, response.getInactivatedCount());

        FileDeleteDetailDto detail = response.getDetails().get(0);
        assertEquals("NOT_FOUND", detail.getAction());
    }

    @Test
    void testSingleDelete_NotFound_ThrowsException() {
        when(fileRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.delete(999L));
    }
}
