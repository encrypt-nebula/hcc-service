package com.example.hcc.controller;

import com.example.hcc.entity.User;
import com.example.hcc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public User create(@RequestBody User user) {
        return service.create(user);
    }

    // ✅ SUPER ADMIN ONLY
    @PostMapping("/super-admin")
    public User createBySuperAdmin(@RequestBody User user) {
        return service.create(user);
    }

    @GetMapping
    public List<User> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return service.update(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
