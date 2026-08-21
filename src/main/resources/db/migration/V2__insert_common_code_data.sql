INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- TEMPERATURE_TYPE: 보관 온도 구분
    ('TEMPERATURE_TYPE_AMBIENT', 'TEMPERATURE_TYPE', '상온', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('TEMPERATURE_TYPE_COLD',    'TEMPERATURE_TYPE', '냉장', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- ZONE_STATUS: 구역 상태
    ('ZONE_STATUS_ACTIVE',   'ZONE_STATUS', '활성', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('ZONE_STATUS_INACTIVE', 'ZONE_STATUS', '비활성', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- PRODUCT_STATUS: 상품 상태
    ('PRODUCT_STATUS_ACTIVE',   'PRODUCT_STATUS', '활성', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('PRODUCT_STATUS_INACTIVE', 'PRODUCT_STATUS', '비활성', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- INBOUND_STATUS: 입고 상태
    ('INBOUND_STATUS_EXPECTED',   'INBOUND_STATUS', '입고 예정', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_WAITING',    'INBOUND_STATUS', '도착 대기', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_PROCESSING', 'INBOUND_STATUS', '검수 중',   3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('INBOUND_STATUS_COMPLETED',  'INBOUND_STATUS', '입고 완료', 4, 1, NOW(6), 'system', NOW(6), 'system'),

    -- OUTBOUND_STATUS: 출고 상태
    ('OUTBOUND_STATUS_REQUESTED',  'OUTBOUND_STATUS', '출고 요청',   1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('OUTBOUND_STATUS_PICKING',    'OUTBOUND_STATUS', '피킹 중',     2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('OUTBOUND_STATUS_INSPECTING', 'OUTBOUND_STATUS', '검수 중',     3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('OUTBOUND_STATUS_COMPLETED',  'OUTBOUND_STATUS', '출고 완료',   4, 1, NOW(6), 'system', NOW(6), 'system'),

    -- RETURN_STATUS: 반품 상태
    ('RETURN_STATUS_RECEIVED',   'RETURN_STATUS', '반품 접수', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('RETURN_STATUS_INSPECTING', 'RETURN_STATUS', '검수 중',   2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('RETURN_STATUS_COMPLETED',  'RETURN_STATUS', '반품 완료', 3, 1, NOW(6), 'system', NOW(6), 'system'),

    -- DISPOSAL_STATUS: 폐기 상태
    ('DISPOSAL_STATUS_REQUESTED', 'DISPOSAL_STATUS', '폐기 요청', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('DISPOSAL_STATUS_APPROVED',  'DISPOSAL_STATUS', '폐기 승인', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('DISPOSAL_STATUS_COMPLETED', 'DISPOSAL_STATUS', '폐기 완료', 3, 1, NOW(6), 'system', NOW(6), 'system'),

    -- LOT_STATUS: Lot 상태
    ('LOT_STATUS_NORMAL',         'LOT_STATUS', '정상',        1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('LOT_STATUS_EXPIRING_SOON',  'LOT_STATUS', '유통기한 임박', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('LOT_STATUS_EXPIRED',        'LOT_STATUS', '유통기한 만료', 3, 1, NOW(6), 'system', NOW(6), 'system'),

    -- QUALITY_STATUS: 재고 품질 상태
    ('QUALITY_STATUS_NORMAL',              'QUALITY_STATUS', '정상',       1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('QUALITY_STATUS_DEFECTIVE',           'QUALITY_STATUS', '불량',       2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('QUALITY_STATUS_DISPOSAL_SCHEDULED',  'QUALITY_STATUS', '폐기 예정',  3, 1, NOW(6), 'system', NOW(6), 'system'),

    -- ALLOCATION_STATUS: 재고 할당 상태
    ('ALLOCATION_STATUS_AVAILABLE', 'ALLOCATION_STATUS', '가용',   1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('ALLOCATION_STATUS_ALLOCATED', 'ALLOCATION_STATUS', '할당 중', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- WORK_AREA_TYPE: 작업 구역 유형
    ('WORK_AREA_TYPE_INBOUND',  'WORK_AREA_TYPE', '입고 처리장',  1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('WORK_AREA_TYPE_OUTBOUND', 'WORK_AREA_TYPE', '출고장',       2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('WORK_AREA_TYPE_RETURN',   'WORK_AREA_TYPE', '반품 처리장',  3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('WORK_AREA_TYPE_DISPOSAL', 'WORK_AREA_TYPE', '폐기 처리장',  4, 1, NOW(6), 'system', NOW(6), 'system');
