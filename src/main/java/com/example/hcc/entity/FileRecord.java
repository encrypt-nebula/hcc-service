package com.example.hcc.entity;

import com.example.hcc.enums.UploadStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@Builder(toBuilder = true)
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Project project;

    private String fileName;

    @Column(name = "s3_path")
    private String s3Path;
    private Integer totalPages;

    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    private String signature;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
