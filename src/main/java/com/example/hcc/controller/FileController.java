package com.example.hcc.controller;

import com.example.hcc.entity.FileRecord;
import com.example.hcc.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    @PostMapping
    public FileRecord create(@RequestBody FileRecord fileRecord) {
        return service.create(fileRecord);
    }

    @GetMapping
    public List<FileRecord> all() {
        return service.getAll();
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
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

