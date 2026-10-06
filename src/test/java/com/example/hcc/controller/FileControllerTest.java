package com.example.hcc.controller;

import com.example.hcc.dto.DeleteFilesRequest;
import com.example.hcc.dto.DeleteFilesResponse;
import com.example.hcc.dto.FileDeleteDetailDto;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.enums.Status;
import com.example.hcc.service.FileService;
import com.example.hcc.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private FileController fileController;

    @Test
    void testAll_WithStatus() {
        FileRecord file = FileRecord.builder().id(1L).fileName("active.pdf").status(Status.ACTIVE).build();
        when(fileService.getAll("ACTIVE")).thenReturn(List.of(file));

        List<FileRecord> result = fileController.all("ACTIVE");
        assertEquals(1, result.size());
        assertEquals("active.pdf", result.get(0).getFileName());
        verify(fileService).getAll("ACTIVE");
    }

    @Test
    void testDelete_SingleFile() {
        FileDeleteDetailDto detail = FileDeleteDetailDto.builder()
                .fileId(1L)
                .action("DELETED_PERMANENTLY")
                .message("No coder assigned. File permanently deleted from database and S3.")
                .build();
        when(fileService.delete(1L)).thenReturn(detail);

        ResponseEntity<FileDeleteDetailDto> response = fileController.delete(1L);
        assertNotNull(response.getBody());
        assertEquals("DELETED_PERMANENTLY", response.getBody().getAction());
        verify(fileService).delete(1L);
    }

    @Test
    void testDeleteFiles_Batch() {
        DeleteFilesRequest request = DeleteFilesRequest.builder().fileIds(List.of(1L, 2L)).build();
        DeleteFilesResponse response = DeleteFilesResponse.builder()
                .totalRequested(2)
                .deletedCount(1)
                .inactivatedCount(1)
                .build();
        when(fileService.deleteFiles(List.of(1L, 2L))).thenReturn(response);

        ResponseEntity<DeleteFilesResponse> res = fileController.deleteFiles(request);
        assertNotNull(res.getBody());
        assertEquals(2, res.getBody().getTotalRequested());
        assertEquals(1, res.getBody().getDeletedCount());
        assertEquals(1, res.getBody().getInactivatedCount());
        verify(fileService).deleteFiles(List.of(1L, 2L));
    }

    @Test
    void testDeleteFilesPost_Batch() {
        DeleteFilesRequest request = DeleteFilesRequest.builder().fileIds(List.of(3L)).build();
        DeleteFilesResponse response = DeleteFilesResponse.builder()
                .totalRequested(1)
                .inactivatedCount(1)
                .build();
        when(fileService.deleteFiles(List.of(3L))).thenReturn(response);

        ResponseEntity<DeleteFilesResponse> res = fileController.deleteFilesPost(request);
        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().getTotalRequested());
        assertEquals(1, res.getBody().getInactivatedCount());
        verify(fileService).deleteFiles(List.of(3L));
    }
}
