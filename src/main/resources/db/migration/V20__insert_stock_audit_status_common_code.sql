INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- STOCK_AUDIT_STATUS: 재고 실사 상태
    ('STOCK_AUDIT_STATUS_SCHEDULED',   'STOCK_AUDIT_STATUS', '실사 예정',   1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('STOCK_AUDIT_STATUS_IN_PROGRESS', 'STOCK_AUDIT_STATUS', '실사 진행중', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('STOCK_AUDIT_STATUS_COMPLETED',   'STOCK_AUDIT_STATUS', '실사 완료',   3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('STOCK_AUDIT_STATUS_CLOSED',      'STOCK_AUDIT_STATUS', '조정 마감',   4, 1, NOW(6), 'system', NOW(6), 'system');
