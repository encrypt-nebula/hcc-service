package com.example.hcc.controller;

import com.example.hcc.dto.ExtractDataResponse;
import com.example.hcc.dto.ExtractDataRequest;
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
}