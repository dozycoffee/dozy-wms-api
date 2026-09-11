-- ZONE_STATUS 코드가 도메인 enum(AvailabilityStatus.AVAILABLE/UNAVAILABLE)과 이름이 어긋나 있던 것을 바로잡는다.
UPDATE common_code SET code = 'ZONE_STATUS_AVAILABLE',   name = '가용'   WHERE code = 'ZONE_STATUS_ACTIVE';
UPDATE common_code SET code = 'ZONE_STATUS_UNAVAILABLE', name = '가용 불가' WHERE code = 'ZONE_STATUS_INACTIVE';

INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- WAREHOUSE_STATUS: 창고 상태
    ('WAREHOUSE_STATUS_AVAILABLE',   'WAREHOUSE_STATUS', '가용',    1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('WAREHOUSE_STATUS_UNAVAILABLE', 'WAREHOUSE_STATUS', '가용 불가', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- WORK_AREA_STATUS: 작업 구역 상태
    ('WORK_AREA_STATUS_AVAILABLE',   'WORK_AREA_STATUS', '가용',    1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('WORK_AREA_STATUS_UNAVAILABLE', 'WORK_AREA_STATUS', '가용 불가', 2, 1, NOW(6), 'system', NOW(6), 'system'),

    -- LOCATION_STATUS: 위치 상태
    ('LOCATION_STATUS_AVAILABLE',   'LOCATION_STATUS', '가용',    1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('LOCATION_STATUS_UNAVAILABLE', 'LOCATION_STATUS', '가용 불가', 2, 1, NOW(6), 'system', NOW(6), 'system');
