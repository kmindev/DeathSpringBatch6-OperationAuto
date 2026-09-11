INSERT INTO victims (name, process_id, terminated_at, status) VALUES
('nginx', '1001', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('chrome', '2002', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'TERMINATED'),
('sleep', '3003', CURRENT_TIMESTAMP, 'RUNNING');
