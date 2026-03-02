package com.example.hcc.dto;

import com.example.hcc.enums.Role;
import com.example.hcc.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String cognitoId;
    private Role role;
    private Status status;
    private Long companyId;
}