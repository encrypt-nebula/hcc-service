package com.example.hcc.entity;

import com.example.hcc.dto.UserResponse;
import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_units")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private FileRecord file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Patient patient;

    @Enumerated(EnumType.STRING)
    private WorkUnitType type;

    private String pageStart;
    private String pageEnd;

    @Enumerated(EnumType.STRING)
    private WorkUnitStatus status;

    private LocalDate dateOfService;

    @Column(name = "assigned_to", columnDefinition = "JSON")
    private String assignedTo;

    private Boolean monitor;
    private Boolean evaluate;
    private Boolean assessOrAddress;
    private Boolean treat;

    @Column(name = "meat_validation", columnDefinition = "JSON")
    private String meatValidation;

    @Column(name = "coder_page_range", columnDefinition = "JSON")
    private String coderPageRange;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
