SET NAMES utf8mb4;

INSERT IGNORE INTO t_order (order_id, user_name, amount, create_time) VALUES
('O1001', 'alice', 128.50, '2026-05-14 10:00:00'),
('O1002', 'bob', 256.00, '2026-05-14 11:00:00'),
('O1003', 'alice', 512.20, '2026-05-14 12:00:00');
