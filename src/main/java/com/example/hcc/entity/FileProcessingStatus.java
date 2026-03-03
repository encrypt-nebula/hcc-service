package com.example.hcc.entity;

import com.example.hcc.enums.FileProcessingStatusType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_processing_status")
@Getter
@Setter
public class FileProcessingStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "s3_path", unique = true)
    private String s3Path;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "project_type")
    private String projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FileProcessingStatusType status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "total_pages")
    private Integer totalPages;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
