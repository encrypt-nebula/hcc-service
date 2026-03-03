package com.example.hcc.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.hcc.enums.CodingSource;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "coding_results")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "manual_icd_code")
    private List<String> manualIcdCode;

    @Column(name = "ai_icd_code")
    private List<String> aiIcdCode;

    @Column(name = "extracted_icd_code")
    private List<String> extractedIcdCode;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "hcc_score")
    private BigDecimal hccScore;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
