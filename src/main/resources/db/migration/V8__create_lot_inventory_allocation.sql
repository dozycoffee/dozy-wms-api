CREATE TABLE lot
(
    lot_id           BIGINT       NOT NULL AUTO_INCREMENT,
    lot_number       VARCHAR(50)  NOT NULL,
    product_id       BIGINT       NOT NULL,
    manufacture_date DATE         NULL,
    expiration_date  DATE         NULL,
    lot_status       VARCHAR(50)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    PRIMARY KEY (lot_id),
    CONSTRAINT fk_lot_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT fk_lot_status FOREIGN KEY (lot_status) REFERENCES common_code (code),
    CONSTRAINT uq_lot_product_number UNIQUE (product_id, lot_number),
    CONSTRAINT chk_lot_expiration_date CHECK (manufacture_date IS NULL OR expiration_date IS NULL OR expiration_date >= manufacture_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inventory
(
    inventory_id       BIGINT       NOT NULL AUTO_INCREMENT,
    product_id         BIGINT       NOT NULL,
    lot_id             BIGINT       NOT NULL,
    location_id        BIGINT       NOT NULL,
    quantity           INT          NOT NULL,
    allocated_quantity INT          NOT NULL DEFAULT 0,
    quality_status      VARCHAR(50)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at         DATETIME(6)  NULL,
    deleted_by          VARCHAR(100) NULL,
    PRIMARY KEY (inventory_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT fk_inventory_lot FOREIGN KEY (lot_id) REFERENCES lot (lot_id),
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) REFERENCES location (location_id),
    CONSTRAINT fk_inventory_quality_status FOREIGN KEY (quality_status) REFERENCES common_code (code),
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_allocated_quantity CHECK (allocated_quantity >= 0 AND allocated_quantity <= quantity)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- idempotency_key: ADR-0008의 (inventory_id, reference_type, reference_id) 조건부(HELD 한정) 유니크 제약.
-- MySQL은 partial/filtered unique index를 지원하지 않으므로, HELD 상태일 때만 값을 채우는 생성 컬럼으로 우회한다.
-- RELEASED/FULFILLED 행은 idempotency_key가 NULL이 되고, MySQL UNIQUE는 NULL을 중복 허용하므로 제약에 걸리지 않는다.
CREATE TABLE allocation
(
    allocation_id   BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_id    BIGINT       NOT NULL,
    reference_type  VARCHAR(50)  NOT NULL,
    reference_id    BIGINT       NOT NULL,
    quantity        INT          NOT NULL,
    status          VARCHAR(50)  NOT NULL,
    idempotency_key VARCHAR(150) AS (
        CASE WHEN status = 'ALLOCATION_STATUS_HELD'
             THEN CONCAT(inventory_id, ':', reference_type, ':', reference_id)
             ELSE NULL END
        ) STORED,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    PRIMARY KEY (allocation_id),
    CONSTRAINT fk_allocation_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id),
    CONSTRAINT fk_allocation_reference_type FOREIGN KEY (reference_type) REFERENCES common_code (code),
    CONSTRAINT fk_allocation_status FOREIGN KEY (status) REFERENCES common_code (code),
    CONSTRAINT chk_allocation_quantity CHECK (quantity > 0),
    CONSTRAINT uq_allocation_held UNIQUE (idempotency_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
