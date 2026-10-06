-- Add status column to files table, defaulting to 'ACTIVE'
ALTER TABLE files ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_files_status ON files (status);
