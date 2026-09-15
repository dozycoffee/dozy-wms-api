INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- DISPOSAL_REASON: 폐기 사유
    ('DISPOSAL_REASON_EXPIRED',           'DISPOSAL_REASON', '유통기한 경과', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('DISPOSAL_REASON_INSPECTION_DEFECT', 'DISPOSAL_REASON', '검수 불량',     2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('DISPOSAL_REASON_RETURN_DEFECT',     'DISPOSAL_REASON', '반품 불량',     3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('DISPOSAL_REASON_OTHER',             'DISPOSAL_REASON', '기타',         4, 1, NOW(6), 'system', NOW(6), 'system');
