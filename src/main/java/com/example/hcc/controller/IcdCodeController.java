package com.example.hcc.controller;

import com.example.hcc.dto.IcdCodesRequest;
import com.example.hcc.entity.IcdCode;
import com.example.hcc.service.IcdCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/icd-codes")
@RequiredArgsConstructor
public class IcdCodeController {

    private final IcdCodeService service;

    @PostMapping
    public IcdCode create(@RequestBody IcdCode code) {
        return service.create(code);
    }

    @GetMapping
    public List<IcdCode> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public IcdCode getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}")
    public IcdCode update(@PathVariable Long id,
            @RequestBody IcdCode code) {
        return service.update(id, code);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/code/{icdCode}")
    public ResponseEntity<IcdCode> getByIcdCode(@PathVariable String icdCode) {
        return service.getByIcdCode(icdCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/codes")
    public List<IcdCode> getByIcdCodes(@RequestBody IcdCodesRequest request) {
        return service.getByIcdCodes(request.getIcdCodes());
    }

    @PostMapping("/validate")
    public com.example.hcc.dto.IcdValidationResponse validateCodes(
            @RequestBody com.example.hcc.dto.IcdValidationRequest request) {
        return new com.example.hcc.dto.IcdValidationResponse(
                service.validateCodes(request.getQueries()));
    }
}
