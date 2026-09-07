CREATE TABLE IF NOT EXISTS user_login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    CONSTRAINT fk_user_login_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_login_logs_user_time (user_id, login_time),
    INDEX idx_user_login_logs_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
