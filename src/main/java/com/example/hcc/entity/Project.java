package com.example.hcc.entity;

import com.example.hcc.auditing.AuditableEntity;
import com.example.hcc.enums.ProjectType;
import com.example.hcc.enums.Status;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    @Enumerated(EnumType.STRING)
    private ProjectType projectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "company_id")
    private Company company;

    private String credentials;

    @Column(name = "review_mode")
    private String reviewMode;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
