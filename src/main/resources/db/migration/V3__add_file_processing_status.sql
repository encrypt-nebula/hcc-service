CREATE TABLE file_processing_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255),
    s3_path VARCHAR(500) UNIQUE,
    project_id INT,
    project_type VARCHAR(20),
    status VARCHAR(20),
    error_message TEXT,
    total_pages INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
