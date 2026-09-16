INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- INVENTORY_HISTORY_TYPE: 재고 이력 유형
    ('INVENTORY_HISTORY_TYPE_INBOUND',  'INVENTORY_HISTORY_TYPE', '입고', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INVENTORY_HISTORY_TYPE_OUTBOUND', 'INVENTORY_HISTORY_TYPE', '출고', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INVENTORY_HISTORY_TYPE_DISPOSAL', 'INVENTORY_HISTORY_TYPE', '폐기', 3, 1, NOW(6), 'system', NOW(6), 'system');
