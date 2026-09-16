CREATE TABLE inventory_history
(
    inventory_history_id BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_id          BIGINT       NOT NULL,
    history_type          VARCHAR(50)  NOT NULL,
    quantity_change        INT          NOT NULL,
    reference_id           BIGINT       NOT NULL,
    created_at             DATETIME(6)  NOT NULL,
    created_by             VARCHAR(100) NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    updated_by             VARCHAR(100) NOT NULL,
    PRIMARY KEY (inventory_history_id),
    CONSTRAINT fk_inventory_history_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id),
    CONSTRAINT fk_inventory_history_type FOREIGN KEY (history_type) REFERENCES common_code (code),
    CONSTRAINT chk_inventory_history_quantity_change CHECK (quantity_change <> 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
