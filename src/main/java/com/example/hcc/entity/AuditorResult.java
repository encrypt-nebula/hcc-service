package com.example.hcc.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auditor_results")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditorResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "work_unit_id")
    private WorkUnit workUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "file_id")
    private FileRecord file;

    @Column(name = "dos")
    private java.time.LocalDate dos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "auditor_id")
    private User auditor;

    @Lob
    @Column(name = "manual_icd_code", columnDefinition = "LONGTEXT")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> manualIcdCode;

    @Lob
    @Column(name = "ai_icd_code", columnDefinition = "LONGTEXT")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> aiIcdCode;

    @Lob
    @Column(name = "extracted_icd_code", columnDefinition = "LONGTEXT")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> extractedIcdCode;

    @Lob
    @Column(name = "submitted_icd_code", columnDefinition = "LONGTEXT")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> submittedIcdCode;

    @Column(name = "hcc_score", precision = 12, scale = 4)
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
