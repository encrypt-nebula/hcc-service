package com.example.hcc.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
// Removed Hibernate-specific JSON mapping annotations to use JPA Convert instead

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
    @JoinColumn(name = "file_id")
    private FileRecord file;

    @Column(name = "dos")
    private java.time.LocalDate dos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JoinColumn(name = "coder_id")
    private User coder;

    @Column(name = "manual_icd_code")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> manualIcdCode;

    @Column(name = "ai_icd_code")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> aiIcdCode;

    @Column(name = "extracted_icd_code")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> extractedIcdCode;

    @Column(name = "submitted_icd_code")
    @Convert(converter = IcdEntryListConverter.class)
    private List<IcdEntry> submittedIcdCode;

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
