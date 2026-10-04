ALTER TABLE inbound_item
    ADD COLUMN expected_lot_number VARCHAR(50) DEFAULT NULL AFTER expected_quantity,
    ADD COLUMN expected_expiration_date DATE DEFAULT NULL AFTER expected_lot_number,
    ADD COLUMN inspection_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' AFTER actual_quantity;

UPDATE inbound_item
SET inspection_status = 'INSPECTED'
WHERE inspection_result <> 'PENDING';

ALTER TABLE inbound_item
    DROP CHECK ck_inbound_item_inspection_result,
    DROP COLUMN inspection_result,
    ADD CONSTRAINT ck_inbound_item_inspection_status CHECK (inspection_status IN ('PENDING', 'INSPECTED'));

CREATE TABLE inbound_receipt
(
    inbound_receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_item_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    manufacture_date DATE DEFAULT NULL,
    expiration_date DATE DEFAULT NULL,
    quantity INT NOT NULL,
    inspection_result VARCHAR(50) NOT NULL,
    defect_reason VARCHAR(50) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (inbound_receipt_id),
    INDEX idx_inbound_receipt_inbound_item (inbound_item_id),
    CONSTRAINT fk_inbound_receipt_inbound_item FOREIGN KEY (inbound_item_id) REFERENCES inbound_item (inbound_item_id),
    CONSTRAINT chk_inbound_receipt_quantity CHECK (quantity > 0),
    CONSTRAINT chk_inbound_receipt_dates CHECK ((manufacture_date IS NULL) OR (expiration_date IS NULL) OR (expiration_date >= manufacture_date)),
    CONSTRAINT ck_inbound_receipt_inspection_result CHECK (inspection_result IN ('NORMAL', 'DEFECTIVE')),
    CONSTRAINT ck_inbound_receipt_defect_reason CHECK (defect_reason IS NULL OR defect_reason IN ('DAMAGED', 'EXPIRED', 'QUALITY', 'OTHER')),
    CONSTRAINT chk_inbound_receipt_defect_reason_result CHECK ((inspection_result = 'DEFECTIVE') = (defect_reason IS NOT NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
