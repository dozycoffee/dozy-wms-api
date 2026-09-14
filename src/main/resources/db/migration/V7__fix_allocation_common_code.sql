-- ADR-0008: Inventory.allocationStatus(AVAILABLE/ALLOCATED) 이진 상태를 Allocation 엔티티로 대체하면서
-- V2에 남아있던 구(舊) ALLOCATION_STATUS 코드를 실제 AllocationStatus enum(HELD/RELEASED/FULFILLED)에 맞게 교체한다.
DELETE FROM common_code WHERE group_code = 'ALLOCATION_STATUS';

INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- ALLOCATION_STATUS: 재고 점유 상태 (ADR-0008)
    ('ALLOCATION_STATUS_HELD',      'ALLOCATION_STATUS', '점유중', 1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('ALLOCATION_STATUS_RELEASED',  'ALLOCATION_STATUS', '해제됨', 2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('ALLOCATION_STATUS_FULFILLED', 'ALLOCATION_STATUS', '완료됨', 3, 1, NOW(6), 'system', NOW(6), 'system'),

    -- ALLOCATION_REFERENCE_TYPE: 점유 요청 주체 (ADR-0008, 현재는 OUTBOUND만 존재)
    ('ALLOCATION_REFERENCE_TYPE_OUTBOUND', 'ALLOCATION_REFERENCE_TYPE', '출고', 1, 1, NOW(6), 'system', NOW(6), 'system');
