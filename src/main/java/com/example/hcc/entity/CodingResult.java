package com.example.hcc.entity;

import com.example.hcc.enums.CodingSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coding_results")
@Getter
@Setter
public class CodingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "work_unit_id")
    private WorkUnit workUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "coder_id")
    private User coder;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "hcc_score")
    private BigDecimal hccScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private CodingSource codingSource;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
