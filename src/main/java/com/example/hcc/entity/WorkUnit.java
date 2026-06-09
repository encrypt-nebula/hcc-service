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


    @Column(name = "coder_page_range", columnDefinition = "JSON")
    private String coderPageRange;

    @Column(name = "monitor")
    private Boolean monitor;

    @Column(name = "evaluate")
    private Boolean evaluate;

    @Column(name = "assess_or_address")
    private Boolean assessOrAddress;

    @Column(name = "treat")
    private Boolean treat;

    @Column(name = "feedback", columnDefinition = "JSON")
    private String feedback;

    @Column(name = "coder_feedback", columnDefinition = "TEXT")
    private String coderFeedback;

    @Column(name = "auditor_message", columnDefinition = "TEXT")
    private String auditorMessage;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
