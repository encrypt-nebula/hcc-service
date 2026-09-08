-- Performance indexes for dashboard queries
CREATE INDEX IF NOT EXISTS idx_files_project_id ON files (project_id);
CREATE INDEX IF NOT EXISTS idx_files_auditor_id ON files (auditor_id);
CREATE INDEX IF NOT EXISTS idx_files_created_at ON files (created_at);

CREATE INDEX IF NOT EXISTS idx_work_units_project_id ON work_units (project_id);
CREATE INDEX IF NOT EXISTS idx_work_units_file_id ON work_units (file_id);
CREATE INDEX IF NOT EXISTS idx_work_units_status ON work_units (status);
CREATE INDEX IF NOT EXISTS idx_work_units_created_at ON work_units (created_at);

CREATE INDEX IF NOT EXISTS idx_coding_results_file_id ON coding_results (file_id);
CREATE INDEX IF NOT EXISTS idx_coding_results_coder_id ON coding_results (coder_id);
CREATE INDEX IF NOT EXISTS idx_coding_results_work_unit_id ON coding_results (work_unit_id);
CREATE INDEX IF NOT EXISTS idx_coding_results_created_at ON coding_results (created_at);

CREATE INDEX IF NOT EXISTS idx_auditor_results_file_id ON auditor_results (file_id);
CREATE INDEX IF NOT EXISTS idx_auditor_results_auditor_id ON auditor_results (auditor_id);
CREATE INDEX IF NOT EXISTS idx_auditor_results_work_unit_id ON auditor_results (work_unit_id);
CREATE INDEX IF NOT EXISTS idx_auditor_results_created_at ON auditor_results (created_at);

CREATE INDEX IF NOT EXISTS idx_user_login_logs_user_id ON user_login_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_user_login_logs_login_time ON user_login_logs (login_time);

CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users (company_id);

CREATE INDEX IF NOT EXISTS idx_projects_created_by ON projects (created_by);
