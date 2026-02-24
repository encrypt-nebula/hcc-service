package com.example.hcc.controller;

import com.example.hcc.entity.Company;
import com.example.hcc.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;

    @PostMapping
    public Company create(@RequestBody Company company) {
        return service.create(company);
    }

    @GetMapping
    public List<Company> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Company get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public Company update(@PathVariable Long id, @RequestBody Company company) {
        return service.update(id, company);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
