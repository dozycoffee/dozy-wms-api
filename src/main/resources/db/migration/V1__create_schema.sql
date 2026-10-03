-- 스키마 baseline. 상태·분류 코드는 컬럼별 CHECK 제약으로 제한한다 (ADR-0013). 외래키는 V2에서 연결한다.

CREATE TABLE warehouse
(
    warehouse_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    warehouse_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (warehouse_id),
    CONSTRAINT ck_warehouse_status CHECK (warehouse_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE zone
(
    zone_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    zone_code VARCHAR(1) NOT NULL,
    zone_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (zone_id),
    CONSTRAINT uq_zone_warehouse_code UNIQUE (warehouse_id, zone_code),
    CONSTRAINT ck_zone_status CHECK (zone_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE work_area
(
    work_area_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    area_code VARCHAR(50) NOT NULL,
    used_capacity INT NOT NULL DEFAULT 0,
    work_area_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (work_area_id),
    CONSTRAINT uq_work_area_warehouse_code UNIQUE (warehouse_id, area_code),
    CONSTRAINT chk_work_area_used_capacity CHECK (used_capacity >= 0),
    CONSTRAINT ck_work_area_status CHECK (work_area_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE location
(
    location_id BIGINT NOT NULL AUTO_INCREMENT,
    zone_id BIGINT NOT NULL,
    location_code VARCHAR(10) NOT NULL,
    max_capacity INT NOT NULL,
    used_capacity INT NOT NULL DEFAULT 0,
    location_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (location_id),
    CONSTRAINT uq_location_zone_code UNIQUE (zone_id, location_code),
    CONSTRAINT chk_location_capacity CHECK ((used_capacity >= 0) AND (used_capacity <= max_capacity)),
    CONSTRAINT ck_location_status CHECK (location_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE product
(
    product_id BIGINT NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    shelf_life_days INT DEFAULT NULL,
    product_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (product_id),
    CONSTRAINT uq_product_code UNIQUE (product_code),
    CONSTRAINT chk_product_shelf_life_days CHECK ((shelf_life_days IS NULL) OR (shelf_life_days >= 0)),
    CONSTRAINT ck_product_category CHECK (category IN ('BEAN', 'SYRUP', 'POWDER', 'DAIRY', 'SUPPLY', 'MD')),
    CONSTRAINT ck_product_status CHECK (product_status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE lot
(
    lot_id BIGINT NOT NULL AUTO_INCREMENT,
    lot_number VARCHAR(50) NOT NULL,
    product_id BIGINT NOT NULL,
    manufacture_date DATE DEFAULT NULL,
    expiration_date DATE DEFAULT NULL,
    lot_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (lot_id),
    CONSTRAINT uq_lot_product_number UNIQUE (product_id, lot_number),
    INDEX idx_lot_expiration_date (expiration_date),
    CONSTRAINT chk_lot_expiration_date CHECK ((manufacture_date IS NULL) OR (expiration_date IS NULL) OR (expiration_date >= manufacture_date)),
    CONSTRAINT ck_lot_status CHECK (lot_status IN ('NORMAL', 'EXPIRING_SOON', 'EXPIRED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inventory
(
    inventory_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    lot_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    allocated_quantity INT NOT NULL DEFAULT 0,
    quality_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (inventory_id),
    INDEX idx_inventory_product_id_quality_status_deleted_at (product_id, quality_status, deleted_at),
    CONSTRAINT chk_inventory_allocated_quantity CHECK ((allocated_quantity >= 0) AND (allocated_quantity <= quantity)),
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_inventory_quality_status CHECK (quality_status IN ('NORMAL', 'DEFECTIVE', 'DISPOSAL_SCHEDULED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- idempotency_key: ADR-0008의 (inventory_id, reference_type, reference_id) 조건부(HELD 한정) 유니크 제약.
-- MySQL은 partial/filtered unique index를 지원하지 않으므로, HELD 상태일 때만 값을 채우는 생성 컬럼으로 우회한다.
-- RELEASED/FULFILLED 행은 idempotency_key가 NULL이 되고, MySQL UNIQUE는 NULL을 중복 허용하므로 제약에 걸리지 않는다.
CREATE TABLE allocation
(
    allocation_id BIGINT NOT NULL AUTO_INCREMENT,
    inventory_id BIGINT NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(150) AS (
        CASE
            WHEN status = 'HELD'
                THEN CONCAT(inventory_id, ':', reference_type, ':', reference_id)
            END
        ) STORED,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (allocation_id),
    CONSTRAINT uq_allocation_held UNIQUE (idempotency_key),
    CONSTRAINT chk_allocation_quantity CHECK (quantity > 0),
    CONSTRAINT ck_allocation_reference_type CHECK (reference_type = 'OUTBOUND'),
    CONSTRAINT ck_allocation_status CHECK (status IN ('HELD', 'RELEASED', 'FULFILLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound
(
    inbound_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    expected_arrival_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (inbound_id),
    CONSTRAINT ck_inbound_status CHECK (status IN ('EXPECTED', 'WAITING', 'PROCESSING', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound_item
(
    inbound_item_id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    expected_quantity INT NOT NULL,
    actual_quantity INT DEFAULT NULL,
    inspection_result VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (inbound_item_id),
    CONSTRAINT chk_inbound_item_actual_quantity CHECK ((actual_quantity IS NULL) OR (actual_quantity >= 0)),
    CONSTRAINT chk_inbound_item_expected_quantity CHECK (expected_quantity > 0),
    CONSTRAINT ck_inbound_item_inspection_result CHECK (inspection_result IN ('PENDING', 'NORMAL', 'DEFECTIVE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound
(
    outbound_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_id),
    CONSTRAINT ck_outbound_status CHECK (status IN ('REQUESTED', 'PICKING', 'INSPECTING', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound_item
(
    outbound_item_id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    requested_quantity INT NOT NULL,
    picked_quantity INT DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_item_id),
    CONSTRAINT chk_outbound_item_picked_quantity CHECK ((picked_quantity IS NULL) OR (picked_quantity >= 0)),
    CONSTRAINT chk_outbound_item_requested_quantity CHECK (requested_quantity > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE return_request
(
    return_request_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (return_request_id),
    CONSTRAINT ck_return_request_status CHECK (status IN ('RECEIVED', 'INSPECTING', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE return_item
(
    return_item_id BIGINT NOT NULL AUTO_INCREMENT,
    return_request_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_quantity INT NOT NULL,
    actual_quantity INT DEFAULT NULL,
    inspection_result VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (return_item_id),
    CONSTRAINT chk_return_item_actual_quantity CHECK ((actual_quantity IS NULL) OR (actual_quantity >= 0)),
    CONSTRAINT chk_return_item_expected_quantity CHECK (expected_quantity > 0),
    CONSTRAINT ck_return_item_inspection_result CHECK (inspection_result IN ('PENDING', 'NORMAL', 'DEFECTIVE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE disposal
(
    disposal_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (disposal_id),
    CONSTRAINT ck_disposal_status CHECK (status IN ('REQUESTED', 'APPROVED', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE disposal_item
(
    disposal_item_id BIGINT NOT NULL AUTO_INCREMENT,
    disposal_id BIGINT NOT NULL,
    inventory_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (disposal_item_id),
    CONSTRAINT chk_disposal_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_disposal_item_reason CHECK (reason IN ('EXPIRED', 'INSPECTION_DEFECT', 'RETURN_DEFECT', 'OTHER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inventory_history
(
    inventory_history_id BIGINT NOT NULL AUTO_INCREMENT,
    inventory_id BIGINT NOT NULL,
    history_type VARCHAR(50) NOT NULL,
    quantity_change INT NOT NULL,
    reference_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (inventory_history_id),
    INDEX idx_inventory_history_inventory_id_created_at (inventory_id, created_at),
    CONSTRAINT chk_inventory_history_quantity_change CHECK (quantity_change <> 0),
    CONSTRAINT ck_inventory_history_type CHECK (history_type IN ('INBOUND', 'OUTBOUND', 'DISPOSAL', 'ADJUSTMENT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_audit
(
    stock_audit_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    assignee VARCHAR(100) DEFAULT NULL,
    approved_by VARCHAR(100) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (stock_audit_id),
    CONSTRAINT ck_stock_audit_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_audit_item
(
    stock_audit_item_id BIGINT NOT NULL AUTO_INCREMENT,
    stock_audit_id BIGINT NOT NULL,
    inventory_id BIGINT NOT NULL,
    snapshot_quantity INT NOT NULL,
    snapshot_taken_at DATETIME(6) NOT NULL,
    counted_quantity INT DEFAULT NULL,
    has_uncommitted_movement TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (stock_audit_item_id),
    CONSTRAINT chk_stock_audit_item_counted_quantity CHECK ((counted_quantity IS NULL) OR (counted_quantity >= 0)),
    CONSTRAINT chk_stock_audit_item_snapshot_quantity CHECK (snapshot_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE warehouse_member
(
    warehouse_member_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    principal_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (warehouse_member_id),
    CONSTRAINT uk_warehouse_member_warehouse_principal UNIQUE (warehouse_id, principal_id),
    INDEX idx_warehouse_member_principal (principal_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
