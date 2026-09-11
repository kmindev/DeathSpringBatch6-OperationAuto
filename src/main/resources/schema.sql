CREATE TABLE IF NOT EXISTS victims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    process_id VARCHAR(50),
    terminated_at TIMESTAMP,
    status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    order_datetime TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    shipping_id VARCHAR(50)
);
