INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- INBOUND_STATUS: 입고 상태
    ('INBOUND_STATUS_EXPECTED',   'INBOUND_STATUS', '입고 예정',   1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_WAITING',    'INBOUND_STATUS', '입고 대기',   2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_PROCESSING', 'INBOUND_STATUS', '입고 처리중', 3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_COMPLETED',  'INBOUND_STATUS', '입고 완료',   4, 1, NOW(6), 'system', NOW(6), 'system'),

    -- INBOUND_ITEM_INSPECTION_RESULT: 입고 상품 검수 결과
    ('INBOUND_ITEM_INSPECTION_RESULT_PENDING',   'INBOUND_ITEM_INSPECTION_RESULT', '검수 대기', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_ITEM_INSPECTION_RESULT_NORMAL',    'INBOUND_ITEM_INSPECTION_RESULT', '정상',      2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_ITEM_INSPECTION_RESULT_DEFECTIVE', 'INBOUND_ITEM_INSPECTION_RESULT', '불량',      3, 1, NOW(6), 'system', NOW(6), 'system');
