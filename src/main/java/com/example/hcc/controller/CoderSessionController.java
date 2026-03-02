package com.example.hcc.controller;

import com.example.hcc.entity.CoderSession;
import com.example.hcc.service.CoderSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coder-sessions")
@RequiredArgsConstructor
public class CoderSessionController {

    private final CoderSessionService service;

    @PostMapping
    public CoderSession create(@RequestBody CoderSession session) {
        return service.create(session);
    }

    @GetMapping
    public List<CoderSession> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CoderSession getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}")
    public CoderSession update(@PathVariable Long id,
                               @RequestBody CoderSession session) {
        return service.update(id, session);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

