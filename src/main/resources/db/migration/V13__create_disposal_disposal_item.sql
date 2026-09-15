CREATE TABLE disposal
(
    disposal_id  BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT       NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    PRIMARY KEY (disposal_id),
    CONSTRAINT fk_disposal_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_disposal_status FOREIGN KEY (status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE disposal_item
(
    disposal_item_id BIGINT       NOT NULL AUTO_INCREMENT,
    disposal_id      BIGINT       NOT NULL,
    inventory_id     BIGINT       NOT NULL,
    quantity         INT          NOT NULL,
    reason           VARCHAR(50)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    PRIMARY KEY (disposal_item_id),
    CONSTRAINT fk_disposal_item_disposal FOREIGN KEY (disposal_id) REFERENCES disposal (disposal_id),
    CONSTRAINT fk_disposal_item_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id),
    CONSTRAINT fk_disposal_item_reason FOREIGN KEY (reason) REFERENCES common_code (code),
    CONSTRAINT chk_disposal_item_quantity CHECK (quantity > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
