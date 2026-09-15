CREATE TABLE inbound
(
    inbound_id            BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id          BIGINT       NOT NULL,
    expected_arrival_date DATE         NOT NULL,
    status                VARCHAR(50)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    created_by            VARCHAR(100) NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    updated_by            VARCHAR(100) NOT NULL,
    PRIMARY KEY (inbound_id),
    CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_inbound_status FOREIGN KEY (status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound_item
(
    inbound_item_id   BIGINT       NOT NULL AUTO_INCREMENT,
    inbound_id        BIGINT       NOT NULL,
    product_id        BIGINT       NOT NULL,
    zone_id           BIGINT       NOT NULL,
    expected_quantity INT          NOT NULL,
    actual_quantity   INT          NULL,
    inspection_result VARCHAR(50)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    PRIMARY KEY (inbound_item_id),
    CONSTRAINT fk_inbound_item_inbound FOREIGN KEY (inbound_id) REFERENCES inbound (inbound_id),
    CONSTRAINT fk_inbound_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT fk_inbound_item_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id),
    CONSTRAINT fk_inbound_item_inspection_result FOREIGN KEY (inspection_result) REFERENCES common_code (code),
    CONSTRAINT chk_inbound_item_expected_quantity CHECK (expected_quantity > 0),
    CONSTRAINT chk_inbound_item_actual_quantity CHECK (actual_quantity IS NULL OR actual_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
