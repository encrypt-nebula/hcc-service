package com.example.hcc.mapper;

import com.example.hcc.cognito.AdminCreateUserRequestModel;
import com.example.hcc.cognito.AdminDeleteUserRequestModel;
import com.example.hcc.dto.UserResponseDto;
import com.example.hcc.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public AdminCreateUserRequestModel mapUserRequest(User user){
        return AdminCreateUserRequestModel.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .groups(List.of(user.getRole()))
                .build();

    }

    public AdminDeleteUserRequestModel mapDeleteRequest(User user){
        return AdminDeleteUserRequestModel.builder()
                .email(user.getEmail())
                .build();

    }

    public UserResponseDto toDto(User user) {
        if (user == null) return null;

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setCognitoId(user.getCognitoId());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());

        if (user.getCompany() != null) {
            dto.setCompanyId(user.getCompany().getId());
        }

        return dto;
    }
}
