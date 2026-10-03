-- common_code 테이블 FK를 컬럼별 CHECK 제약으로 대체하고 테이블을 제거한다 (ADR-0013).
-- 값은 {GROUP}_{VALUE} 형식을 그대로 유지한다.

ALTER TABLE warehouse DROP FOREIGN KEY fk_warehouse_status;
ALTER TABLE warehouse ADD CONSTRAINT ck_warehouse_status
    CHECK (warehouse_status IN ('WAREHOUSE_STATUS_AVAILABLE', 'WAREHOUSE_STATUS_UNAVAILABLE'));

ALTER TABLE zone DROP FOREIGN KEY fk_zone_status;
ALTER TABLE zone ADD CONSTRAINT ck_zone_status
    CHECK (zone_status IN ('ZONE_STATUS_AVAILABLE', 'ZONE_STATUS_UNAVAILABLE'));

ALTER TABLE work_area DROP FOREIGN KEY fk_work_area_status;
ALTER TABLE work_area ADD CONSTRAINT ck_work_area_status
    CHECK (work_area_status IN ('WORK_AREA_STATUS_AVAILABLE', 'WORK_AREA_STATUS_UNAVAILABLE'));

ALTER TABLE location DROP FOREIGN KEY fk_location_status;
ALTER TABLE location ADD CONSTRAINT ck_location_status
    CHECK (location_status IN ('LOCATION_STATUS_AVAILABLE', 'LOCATION_STATUS_UNAVAILABLE'));

ALTER TABLE product DROP FOREIGN KEY fk_product_category;
ALTER TABLE product ADD CONSTRAINT ck_product_category
    CHECK (category IN ('PRODUCT_CATEGORY_BEAN', 'PRODUCT_CATEGORY_SYRUP', 'PRODUCT_CATEGORY_POWDER',
                        'PRODUCT_CATEGORY_DAIRY', 'PRODUCT_CATEGORY_SUPPLY', 'PRODUCT_CATEGORY_MD'));

ALTER TABLE product DROP FOREIGN KEY fk_product_status;
ALTER TABLE product ADD CONSTRAINT ck_product_status
    CHECK (product_status IN ('PRODUCT_STATUS_ACTIVE', 'PRODUCT_STATUS_INACTIVE'));

ALTER TABLE lot DROP FOREIGN KEY fk_lot_status;
ALTER TABLE lot ADD CONSTRAINT ck_lot_status
    CHECK (lot_status IN ('LOT_STATUS_NORMAL', 'LOT_STATUS_EXPIRING_SOON', 'LOT_STATUS_EXPIRED'));

ALTER TABLE inventory DROP FOREIGN KEY fk_inventory_quality_status;
ALTER TABLE inventory ADD CONSTRAINT ck_inventory_quality_status
    CHECK (quality_status IN ('QUALITY_STATUS_NORMAL', 'QUALITY_STATUS_DEFECTIVE', 'QUALITY_STATUS_DISPOSAL_SCHEDULED'));

ALTER TABLE allocation DROP FOREIGN KEY fk_allocation_reference_type;
ALTER TABLE allocation ADD CONSTRAINT ck_allocation_reference_type
    CHECK (reference_type IN ('ALLOCATION_REFERENCE_TYPE_OUTBOUND'));

ALTER TABLE allocation DROP FOREIGN KEY fk_allocation_status;
ALTER TABLE allocation ADD CONSTRAINT ck_allocation_status
    CHECK (status IN ('ALLOCATION_STATUS_HELD', 'ALLOCATION_STATUS_RELEASED', 'ALLOCATION_STATUS_FULFILLED'));

ALTER TABLE inbound DROP FOREIGN KEY fk_inbound_status;
ALTER TABLE inbound ADD CONSTRAINT ck_inbound_status
    CHECK (status IN ('INBOUND_STATUS_EXPECTED', 'INBOUND_STATUS_WAITING', 'INBOUND_STATUS_PROCESSING',
                      'INBOUND_STATUS_COMPLETED'));

ALTER TABLE inbound_item DROP FOREIGN KEY fk_inbound_item_inspection_result;
ALTER TABLE inbound_item ADD CONSTRAINT ck_inbound_item_inspection_result
    CHECK (inspection_result IN ('INBOUND_ITEM_INSPECTION_RESULT_PENDING', 'INBOUND_ITEM_INSPECTION_RESULT_NORMAL',
                                 'INBOUND_ITEM_INSPECTION_RESULT_DEFECTIVE'));

ALTER TABLE outbound DROP FOREIGN KEY fk_outbound_status;
ALTER TABLE outbound ADD CONSTRAINT ck_outbound_status
    CHECK (status IN ('OUTBOUND_STATUS_REQUESTED', 'OUTBOUND_STATUS_PICKING', 'OUTBOUND_STATUS_INSPECTING',
                      'OUTBOUND_STATUS_COMPLETED'));

ALTER TABLE return_request DROP FOREIGN KEY fk_return_request_status;
ALTER TABLE return_request ADD CONSTRAINT ck_return_request_status
    CHECK (status IN ('RETURN_STATUS_RECEIVED', 'RETURN_STATUS_INSPECTING', 'RETURN_STATUS_COMPLETED'));

ALTER TABLE return_item DROP FOREIGN KEY fk_return_item_inspection_result;
ALTER TABLE return_item ADD CONSTRAINT ck_return_item_inspection_result
    CHECK (inspection_result IN ('RETURN_ITEM_INSPECTION_RESULT_PENDING', 'RETURN_ITEM_INSPECTION_RESULT_NORMAL',
                                 'RETURN_ITEM_INSPECTION_RESULT_DEFECTIVE'));

ALTER TABLE disposal DROP FOREIGN KEY fk_disposal_status;
ALTER TABLE disposal ADD CONSTRAINT ck_disposal_status
    CHECK (status IN ('DISPOSAL_STATUS_REQUESTED', 'DISPOSAL_STATUS_APPROVED', 'DISPOSAL_STATUS_COMPLETED'));

ALTER TABLE disposal_item DROP FOREIGN KEY fk_disposal_item_reason;
ALTER TABLE disposal_item ADD CONSTRAINT ck_disposal_item_reason
    CHECK (reason IN ('DISPOSAL_REASON_EXPIRED', 'DISPOSAL_REASON_INSPECTION_DEFECT',
                      'DISPOSAL_REASON_RETURN_DEFECT', 'DISPOSAL_REASON_OTHER'));

ALTER TABLE inventory_history DROP FOREIGN KEY fk_inventory_history_type;
ALTER TABLE inventory_history ADD CONSTRAINT ck_inventory_history_type
    CHECK (history_type IN ('INVENTORY_HISTORY_TYPE_INBOUND', 'INVENTORY_HISTORY_TYPE_OUTBOUND',
                            'INVENTORY_HISTORY_TYPE_DISPOSAL', 'INVENTORY_HISTORY_TYPE_ADJUSTMENT'));

ALTER TABLE stock_audit DROP FOREIGN KEY fk_stock_audit_status;
ALTER TABLE stock_audit ADD CONSTRAINT ck_stock_audit_status
    CHECK (status IN ('STOCK_AUDIT_STATUS_SCHEDULED', 'STOCK_AUDIT_STATUS_IN_PROGRESS',
                      'STOCK_AUDIT_STATUS_COMPLETED', 'STOCK_AUDIT_STATUS_CLOSED'));

DROP TABLE common_code;
