INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- INBOUND_ITEM_INSPECTION_RESULT: 입고 상품 검수 결과
    ('INBOUND_ITEM_INSPECTION_RESULT_PENDING',   'INBOUND_ITEM_INSPECTION_RESULT', '검수 대기', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_ITEM_INSPECTION_RESULT_NORMAL',    'INBOUND_ITEM_INSPECTION_RESULT', '정상',      2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_ITEM_INSPECTION_RESULT_DEFECTIVE', 'INBOUND_ITEM_INSPECTION_RESULT', '불량',      3, 1, NOW(6), 'system', NOW(6), 'system');
