package com.example.hcc.repository;

import com.example.hcc.entity.FileProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileProcessingStatusRepository extends JpaRepository<FileProcessingStatus, Long> {

    Optional<FileProcessingStatus> findByS3Path(String s3Path);
}
