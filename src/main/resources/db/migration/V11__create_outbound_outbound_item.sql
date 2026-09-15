CREATE TABLE outbound
(
    outbound_id  BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT       NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_id),
    CONSTRAINT fk_outbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_outbound_status FOREIGN KEY (status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound_item
(
    outbound_item_id  BIGINT       NOT NULL AUTO_INCREMENT,
    outbound_id       BIGINT       NOT NULL,
    product_id        BIGINT       NOT NULL,
    requested_quantity INT         NOT NULL,
    picked_quantity   INT          NULL,
    created_at        DATETIME(6)  NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_item_id),
    CONSTRAINT fk_outbound_item_outbound FOREIGN KEY (outbound_id) REFERENCES outbound (outbound_id),
    CONSTRAINT fk_outbound_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    CONSTRAINT chk_outbound_item_requested_quantity CHECK (requested_quantity > 0),
    CONSTRAINT chk_outbound_item_picked_quantity CHECK (picked_quantity IS NULL OR picked_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
