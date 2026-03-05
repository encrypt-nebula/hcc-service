package com.example.hcc.controller;

import com.example.hcc.dto.FileStatusUpdateDto;
import com.example.hcc.service.FileStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files/status")
@RequiredArgsConstructor
public class FileStatusController {

    private final FileStatusService fileStatusService;

    @Value("${internal.api-key}")
    private String expectedApiKey;

    @PutMapping
    public ResponseEntity<?> updateFileStatus(
            @RequestHeader("X-Internal-Service-Key") String apiKey,
            @RequestBody FileStatusUpdateDto dto) {

        System.out.println("Incoming File Status Payload: " + dto);

        if (!expectedApiKey.equals(apiKey)) {
            System.out.println(
                    "Unauthorized access attempt. Expected key: " + expectedApiKey + ", but received: " + apiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Service Key");
        }

        try {
            fileStatusService.updateOrCreateStatus(dto);
            return ResponseEntity.ok("File status successfully updated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating file status: " + e.getMessage());
        }
    }
}
