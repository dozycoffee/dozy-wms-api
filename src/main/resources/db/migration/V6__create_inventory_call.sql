CREATE TABLE inventory_call
(
    inventory_call_id BIGINT NOT NULL AUTO_INCREMENT,
    call_type VARCHAR(50) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_path VARCHAR(200) NOT NULL,
    request_body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(500) DEFAULT NULL,
    response_body TEXT DEFAULT NULL,
    sent_at DATETIME(6) DEFAULT NULL,
    dead_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (inventory_call_id),
    CONSTRAINT uq_inventory_call_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_inventory_call_dispatch (warehouse_id, status, inventory_call_id),
    INDEX idx_inventory_call_status_created (status, inventory_call_id),
    CONSTRAINT fk_inventory_call_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT chk_inventory_call_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_inventory_call_type CHECK (call_type IN ('INBOUND_RECEIPT', 'RETURN', 'DISPOSAL', 'FULFILLMENT', 'REALLOCATION', 'RELEASE', 'STOCK_ADJUSTMENT')),
    CONSTRAINT ck_inventory_call_status CHECK (status IN ('PENDING', 'SENT', 'DEAD', 'DISCARDED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
