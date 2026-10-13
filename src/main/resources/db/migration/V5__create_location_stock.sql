CREATE TABLE location_stock
(
    location_stock_id BIGINT NOT NULL AUTO_INCREMENT,
    location_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    quality_status VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (location_stock_id),
    CONSTRAINT uq_location_stock_key UNIQUE (location_id, product_id, lot_number, quality_status),
    INDEX idx_location_stock_product_lot_quality (product_id, lot_number, quality_status),
    CONSTRAINT fk_location_stock_location FOREIGN KEY (location_id) REFERENCES location (location_id),
    CONSTRAINT fk_location_stock_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT chk_location_stock_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_location_stock_quality_status CHECK (quality_status IN ('NORMAL', 'DEFECTIVE', 'DISPOSAL_SCHEDULED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
