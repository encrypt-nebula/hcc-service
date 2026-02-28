package com.example.hcc.controller;

import com.example.hcc.entity.Patient;
import com.example.hcc.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService service;

    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        return service.create(patient);
    }

    @GetMapping
    public List<Patient> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public Patient update(@PathVariable Long id, @RequestBody Patient patient) {
        return service.update(id, patient);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/fetch/{fileId}")
    public Patient getByFileId(@PathVariable("fileId") Long id) {
        return service.getByFile(id);
    }
}

