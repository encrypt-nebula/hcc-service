package com.example.hcc.entity;

import com.example.hcc.enums.Role;
import com.example.hcc.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void prePersistAndUpdate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (password != null && !password.isBlank() &&
                !password.startsWith("$2a$") && !password.startsWith("$2b$")) {
        }
    }
}

