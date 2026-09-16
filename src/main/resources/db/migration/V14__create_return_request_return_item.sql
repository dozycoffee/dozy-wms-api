CREATE TABLE return_request
(
    return_request_id BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id       BIGINT       NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100) NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    updated_by         VARCHAR(100) NOT NULL,
    PRIMARY KEY (return_request_id),
    CONSTRAINT fk_return_request_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_return_request_status FOREIGN KEY (status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE return_item
(
    return_item_id     BIGINT       NOT NULL AUTO_INCREMENT,
    return_request_id  BIGINT       NOT NULL,
    product_id         BIGINT       NOT NULL,
    expected_quantity  INT          NOT NULL,
    actual_quantity    INT          NULL,
    inspection_result  VARCHAR(50)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100) NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    updated_by         VARCHAR(100) NOT NULL,
    PRIMARY KEY (return_item_id),
    CONSTRAINT fk_return_item_return_request FOREIGN KEY (return_request_id) REFERENCES return_request (return_request_id),
    CONSTRAINT fk_return_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT fk_return_item_inspection_result FOREIGN KEY (inspection_result) REFERENCES common_code (code),
    CONSTRAINT chk_return_item_expected_quantity CHECK (expected_quantity > 0),
    CONSTRAINT chk_return_item_actual_quantity CHECK (actual_quantity IS NULL OR actual_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
