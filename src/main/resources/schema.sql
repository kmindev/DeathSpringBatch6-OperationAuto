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

CREATE TABLE posts (
                       id BIGINT NOT NULL,
                       title VARCHAR(255),
                       content TEXT,
                       writer VARCHAR(255),
                       PRIMARY KEY (id)
);

CREATE TABLE reports (
                         id BIGINT NOT NULL,
                         post_id BIGINT NOT NULL,
                         report_type VARCHAR(50),
                         reporter_level INTEGER,
                         evidence_data TEXT,
                         reported_at TIMESTAMP,
                         PRIMARY KEY (id),
                         FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE exterminated_posts (
    post_id BIGINT PRIMARY KEY,
    writer VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    report_count INTEGER NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    exterminated_at TIMESTAMP NOT NULL
);
