-- 개발 DB에 창고 구조(창고 1, WorkArea 4, Zone 6, Location 15)를 적재한다.
-- 창고가 이미 하나라도 있으면 아무것도 하지 않는다(재실행해도 안전하다).
-- 값은 CLAUDE.md의 "Warehouse Capacity Spec"과 같다: WorkArea 680 + Zone 4,320 = 5,000.
-- WorkArea의 용량(AreaCode)과 Zone의 용량(ZoneCode)은 코드에 있고 이 스크립트는 Location의 최대 용량만 정한다.

SET @now = NOW(6);
SET @empty = (SELECT COUNT(*) = 0 FROM warehouse);

INSERT INTO warehouse (warehouse_name, address, latitude, longitude, warehouse_status, created_at, created_by, updated_at, updated_by)
SELECT 'DOZY COFFEE 중앙 물류센터', '경기도 이천시 부발읍 물류단지로 100', 37.279200, 127.442500, 'AVAILABLE', @now, 'dev-seed', @now, 'dev-seed'
FROM DUAL
WHERE @empty = 1;

SET @warehouse_id = IF(@empty = 1, LAST_INSERT_ID(), NULL);

INSERT INTO work_area (warehouse_id, area_code, used_capacity, work_area_status, created_at, created_by, updated_at, updated_by)
SELECT @warehouse_id, v.area_code, 0, 'AVAILABLE', @now, 'dev-seed', @now, 'dev-seed'
FROM (
    SELECT 'INBOUND' AS area_code
    UNION ALL SELECT 'OUTBOUND'
    UNION ALL SELECT 'RETURN'
    UNION ALL SELECT 'DISPOSAL'
) v
WHERE @warehouse_id IS NOT NULL;

INSERT INTO zone (warehouse_id, zone_code, zone_status, created_at, created_by, updated_at, updated_by)
SELECT @warehouse_id, v.zone_code, 'AVAILABLE', @now, 'dev-seed', @now, 'dev-seed'
FROM (
    SELECT 'A' AS zone_code
    UNION ALL SELECT 'B'
    UNION ALL SELECT 'C'
    UNION ALL SELECT 'D'
    UNION ALL SELECT 'E'
    UNION ALL SELECT 'F'
) v
WHERE @warehouse_id IS NOT NULL;

INSERT INTO location (zone_id, location_code, max_capacity, used_capacity, location_status, created_at, created_by, updated_at, updated_by)
SELECT z.zone_id, v.location_code, v.max_capacity, 0, 'AVAILABLE', @now, 'dev-seed', @now, 'dev-seed'
FROM (
    SELECT 'A' AS zone_code, 'A-01' AS location_code, 320 AS max_capacity
    UNION ALL SELECT 'A', 'A-02', 270
    UNION ALL SELECT 'A', 'A-03', 230
    UNION ALL SELECT 'B', 'B-01', 270
    UNION ALL SELECT 'B', 'B-02', 280
    UNION ALL SELECT 'C', 'C-01', 230
    UNION ALL SELECT 'C', 'C-02', 230
    UNION ALL SELECT 'D', 'D-01', 180
    UNION ALL SELECT 'D', 'D-02', 180
    UNION ALL SELECT 'E', 'E-01', 480
    UNION ALL SELECT 'E', 'E-02', 450
    UNION ALL SELECT 'E', 'E-03', 420
    UNION ALL SELECT 'E', 'E-04', 360
    UNION ALL SELECT 'F', 'F-01', 230
    UNION ALL SELECT 'F', 'F-02', 190
) v
JOIN zone z ON z.warehouse_id = @warehouse_id AND z.zone_code = v.zone_code
WHERE @warehouse_id IS NOT NULL;
