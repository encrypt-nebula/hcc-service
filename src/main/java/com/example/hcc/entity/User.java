package com.example.hcc.entity;

import com.example.hcc.enums.Role;
import com.example.hcc.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User{

    private static final PasswordEncoder PASSWORD_ENCODER =
            new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Column(name = "cognito_id")
    private String cognitoId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @PrePersist
    @PreUpdate
    private void hashPassword() {
        if (password == null || password.isBlank()) {
            return;
        }

        if (password.startsWith("$2a$") || password.startsWith("$2b$")) {
            return;
        }

        this.password = PASSWORD_ENCODER.encode(this.password);
    }
}

