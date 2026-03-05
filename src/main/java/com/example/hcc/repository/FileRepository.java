package com.example.hcc.repository;

import com.example.hcc.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByS3Path(String s3Path);
}
