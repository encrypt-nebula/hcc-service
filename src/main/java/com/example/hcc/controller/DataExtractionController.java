package com.example.hcc.controller;

import com.example.hcc.dto.DataExtractionDto;
import com.example.hcc.service.DataExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract-data")
@RequiredArgsConstructor
public class DataExtractionController {

    private final DataExtractionService dataExtractionService;

    @Value("${internal.api-key}")
    private String expectedApiKey;

    @PostMapping
    public ResponseEntity<?> extractData(
            @RequestHeader("X-Internal-Service-Key") String apiKey,
            @RequestBody DataExtractionDto dto) {

        System.out.println("Incoming Extract Data Payload: " + dto);

        if (!expectedApiKey.equals(apiKey)) {
            System.out.println("Unauthorized access attempt on /extract-data. Expected key: " + expectedApiKey
                    + ", but received: " + apiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Service Key");
        }

        try {
            dataExtractionService.processExtraction(dto);
            return ResponseEntity.ok("Data successfully received and processed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data: " + e.getMessage());
        }
    }
}
