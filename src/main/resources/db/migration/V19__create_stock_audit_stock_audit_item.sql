CREATE TABLE stock_audit
(
    stock_audit_id BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id   BIGINT       NOT NULL,
    zone_id        BIGINT       NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    assignee       VARCHAR(100) NULL,
    approved_by    VARCHAR(100) NULL,
    created_at     DATETIME(6)  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    PRIMARY KEY (stock_audit_id),
    CONSTRAINT fk_stock_audit_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_stock_audit_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id),
    CONSTRAINT fk_stock_audit_status FOREIGN KEY (status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_audit_item
(
    stock_audit_item_id      BIGINT       NOT NULL AUTO_INCREMENT,
    stock_audit_id           BIGINT       NOT NULL,
    inventory_id              BIGINT       NOT NULL,
    snapshot_quantity         INT          NOT NULL,
    snapshot_taken_at         DATETIME(6)  NOT NULL,
    counted_quantity          INT          NULL,
    has_uncommitted_movement  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at                DATETIME(6)  NOT NULL,
    created_by                VARCHAR(100) NOT NULL,
    updated_at                DATETIME(6)  NOT NULL,
    updated_by                VARCHAR(100) NOT NULL,
    PRIMARY KEY (stock_audit_item_id),
    CONSTRAINT fk_stock_audit_item_stock_audit FOREIGN KEY (stock_audit_id) REFERENCES stock_audit (stock_audit_id),
    CONSTRAINT fk_stock_audit_item_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id),
    CONSTRAINT chk_stock_audit_item_snapshot_quantity CHECK (snapshot_quantity >= 0),
    CONSTRAINT chk_stock_audit_item_counted_quantity CHECK (counted_quantity IS NULL OR counted_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
