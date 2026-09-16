INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- RETURN_ITEM_INSPECTION_RESULT: 반품 상품 검수 결과
    ('RETURN_ITEM_INSPECTION_RESULT_PENDING',   'RETURN_ITEM_INSPECTION_RESULT', '검수 대기', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('RETURN_ITEM_INSPECTION_RESULT_NORMAL',    'RETURN_ITEM_INSPECTION_RESULT', '정상',      2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('RETURN_ITEM_INSPECTION_RESULT_DEFECTIVE', 'RETURN_ITEM_INSPECTION_RESULT', '불량',      3, 1, NOW(6), 'system', NOW(6), 'system');
