package com.example.hcc.service;

import com.example.hcc.cognito.AdminCreateUserRequestModel;
import com.example.hcc.cognito.AdminDeleteUserRequestModel;
import com.example.hcc.cognito.CognitoService;
import com.example.hcc.entity.User;
import com.example.hcc.exceptions.ResourceNotFoundException;
import com.example.hcc.mapper.UserMapper;
import com.example.hcc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final CognitoService cognitoService;
    private final UserMapper userMapper;

    public User create(User user) {
        AdminCreateUserRequestModel adminCreateUserRequestModel = userMapper.mapUserRequest(user);
        String cognitoId = cognitoService.adminCreateUser(adminCreateUserRequestModel);
        user.setCognitoId(cognitoId);
        try{
            return repo.save(user);

        }catch(DataIntegrityViolationException e){
            cognitoService.adminDeleteUser(userMapper.mapDeleteRequest(user));
            throw e;
        }
    }

    public List<User> getAll() {
        return repo.findAll();
    }

    public User get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User update(Long id, User incoming) {

        User existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }

        if (incoming.getEmail() != null) {
            existing.setEmail(incoming.getEmail());
        }

        if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
            existing.setPassword(incoming.getPassword());
        }

        if (incoming.getRole() != null) {
            existing.setRole(incoming.getRole());
        }

        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }

        if (incoming.getCompany() != null) {
            existing.setCompany(incoming.getCompany());
        }

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

