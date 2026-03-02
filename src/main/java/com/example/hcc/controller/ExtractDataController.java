package com.example.hcc.controller;

import com.example.hcc.dto.ExtractDataResponse;
import com.example.hcc.dto.ExtractDataRequest;
import com.example.hcc.service.ExtractDataDeleteService;
import com.example.hcc.service.ExtractDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract-data")
@RequiredArgsConstructor
public class ExtractDataController {

    private final ExtractDataService service;
    private final ExtractDataDeleteService deleteService;

    @PostMapping
    public ResponseEntity<ExtractDataResponse> extractData(
            @Valid @RequestBody ExtractDataRequest request) {

        service.process(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ExtractDataResponse(
                        "SUCCESS",
                        "Data updated successfully"
                ));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteExtractData(@PathVariable Long fileId) {

        deleteService.deleteExtractData(fileId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ExtractDataResponse(
                        "SUCCESS",
                        "Data Deleted successfully"
                ));
    }
}