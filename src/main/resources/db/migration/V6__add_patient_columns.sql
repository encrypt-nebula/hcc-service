ALTER TABLE patients 
ADD COLUMN hcin_number VARCHAR(100),
ADD COLUMN member_id VARCHAR(100),
ADD COLUMN physician_name VARCHAR(255),
ADD COLUMN signed_at DATE;
