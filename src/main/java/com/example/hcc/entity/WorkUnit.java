package com.example.hcc.entity;

import com.example.hcc.enums.WorkUnitStatus;
import com.example.hcc.enums.WorkUnitType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    private Integer pageStart;
    private Integer pageEnd;

    @Enumerated(EnumType.STRING)
    private WorkUnitStatus status;

    private java.time.LocalDate dateOfService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    private Boolean monitor;
    private Boolean evaluate;
    private Boolean assessOrAddress;
    private Boolean treat;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
