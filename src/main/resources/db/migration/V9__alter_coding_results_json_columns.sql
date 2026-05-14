ALTER TABLE coding_results MODIFY COLUMN manual_icd_code LONGTEXT;
ALTER TABLE coding_results MODIFY COLUMN ai_icd_code LONGTEXT;
ALTER TABLE coding_results MODIFY COLUMN extracted_icd_code LONGTEXT;
ALTER TABLE coding_results MODIFY COLUMN submitted_icd_code LONGTEXT;
ALTER TABLE coding_results MODIFY COLUMN hcc_score DECIMAL(12,4);
ALTER TABLE icd_codes MODIFY COLUMN hcc_score DECIMAL(12,4);
ALTER TABLE hcc_scores MODIFY COLUMN hcc_score DECIMAL(12,4);
