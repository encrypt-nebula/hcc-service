-- Add AUDITOR column to files table
ALTER TABLE files ADD COLUMN auditor_id BIGINT;
ALTER TABLE files ADD CONSTRAINT fk_files_auditor FOREIGN KEY (auditor_id) REFERENCES users(id);

-- Create auditor_results table
CREATE TABLE auditor_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_unit_id BIGINT,
    file_id BIGINT,
    auditor_id BIGINT,
    dos DATE,
    manual_icd_code LONGTEXT,
    ai_icd_code LONGTEXT,
    extracted_icd_code LONGTEXT,
    submitted_icd_code LONGTEXT,
    hcc_score DECIMAL(12, 4),
    created_at DATETIME,
    CONSTRAINT fk_auditor_results_work_unit FOREIGN KEY (work_unit_id) REFERENCES work_units(id),
    CONSTRAINT fk_auditor_results_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_auditor_results_auditor FOREIGN KEY (auditor_id) REFERENCES users(id)
);
