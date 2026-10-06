package com.example.hcc.controller;

import com.example.hcc.dto.DeleteFilesRequest;
import com.example.hcc.dto.DeleteFilesResponse;
import com.example.hcc.dto.FileDeleteDetailDto;
import com.example.hcc.dto.PresignedUrlRequest;
import com.example.hcc.dto.PresignedUrlResponse;
import com.example.hcc.entity.FileRecord;
import com.example.hcc.service.FileService;
import com.example.hcc.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    private final S3PresignedUrlService s3PresignedUrlService;

    @PostMapping
    public FileRecord create(@RequestBody FileRecord fileRecord) {
        return service.create(fileRecord);
    }

    @GetMapping
    public List<FileRecord> all(@RequestParam(value = "status", required = false) String status) {
        return service.getAll(status);
    }

    @GetMapping("/{id}")
    public FileRecord get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public FileRecord update(@PathVariable Long id, @RequestBody FileRecord fileRecord) {
        return service.update(id, fileRecord);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<FileDeleteDetailDto> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }

    @DeleteMapping
    public ResponseEntity<DeleteFilesResponse> deleteFiles(@Valid @RequestBody DeleteFilesRequest request) {
        return ResponseEntity.ok(service.deleteFiles(request.getFileIds()));
    }

    @PostMapping("/delete")
    public ResponseEntity<DeleteFilesResponse> deleteFilesPost(@Valid @RequestBody DeleteFilesRequest request) {
        return ResponseEntity.ok(service.deleteFiles(request.getFileIds()));
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestBody PresignedUrlRequest request) {

        String url = s3PresignedUrlService.generatePresignedUrl(request.getS3Path());
        return ResponseEntity.ok(new PresignedUrlResponse(url));
    }
}
