CREATE TABLE IF NOT EXISTS victims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    process_id VARCHAR(50),
    terminated_at TIMESTAMP,
    status VARCHAR(20)
);
