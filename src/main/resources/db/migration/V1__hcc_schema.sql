-- USERS
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    email VARCHAR(150) UNIQUE,
    password VARCHAR(255),
    cognito_id VARCHAR(255),
    role VARCHAR(20),          -- ADMIN, TL, CODER
    status VARCHAR(20), -- ACTIVE, INACTIVE
    company_id BIGINT
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (company_id) REFERENCES company(id)

);

--Company
CREATE TABLE company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150),
    address VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PROJECTS
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(150),
    project_type VARCHAR(20),  -- PROSPECTIVE, RETROSPECTIVE
    created_by BIGINT,
    credentials VARCHAR(150)
    review_mode VARCHAR(150)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- FILES
CREATE TABLE files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    file_name VARCHAR(255),
    s3_path VARCHAR(500),
    total_pages INT,
    upload_status VARCHAR(20), -- QUEUED, PROCESSED, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- PATIENTS
CREATE TABLE patients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    file_id BIGINT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    dob DATE,
    dos DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (file_id) REFERENCES files(id)
);

-- WORK UNITS (CORE)
CREATE TABLE work_units (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    file_id BIGINT,
    patient_id BIGINT NULL,
    type VARCHAR(20),          -- PATIENT, PAGE_RANGE
    page_start INT NULL,
    page_end INT NULL,
    status VARCHAR(30),        -- UNASSIGNED, ASSIGNED, IN_PROGRESS, COMPLETED
    assigned_to BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (file_id) REFERENCES files(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
);

-- CODER SESSIONS
CREATE TABLE coder_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_unit_id BIGINT,
    coder_id BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (work_unit_id) REFERENCES work_units(id),
    FOREIGN KEY (coder_id) REFERENCES users(id)
);

-- ICD MASTER
CREATE TABLE icd_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    icd_code VARCHAR(20),
    description TEXT
);

-- HCC MASTER
CREATE TABLE hcc_scores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    icd_code VARCHAR(20),
    hcc_score DECIMAL(5,3)
);

-- CODING RESULTS
CREATE TABLE coding_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_unit_id BIGINT,
    coder_id BIGINT,
    icd_code VARCHAR(20),
    hcc_score DECIMAL(5,3),
    source VARCHAR(10),        -- AI, MANUAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (work_unit_id) REFERENCES work_units(id),
    FOREIGN KEY (coder_id) REFERENCES users(id)
);
