INSERT INTO victims (name, process_id, terminated_at, status) VALUES
('nginx', '1001', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('chrome', '2002', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('sleep', '3003', CURRENT_TIMESTAMP, 'RUNNING');

INSERT INTO orders (customer_id, order_datetime, status, shipping_id)
SELECT
    FLOOR(RAND() * 100 + 1),
    DATEADD('DAY', -CAST(RAND() * 30 AS INT), CURRENT_TIMESTAMP),
    CASE
        WHEN rn <= 10 THEN 'READY_FOR_SHIPMENT'
        WHEN rn <= 20 THEN 'SHIPPED'
        ELSE 'CANCELLED'
    END,
    CASE
        WHEN rn <= 10 THEN NULL
        WHEN rn <= 20 THEN NULL
        ELSE 'SHIP-' || LPAD(CAST(rn AS VARCHAR), 8, '0')
    END
FROM (
    SELECT X AS rn FROM SYSTEM_RANGE(1, 30)
) AS series;
