CREATE TABLE warehouse_member
(
    warehouse_member_id BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id        BIGINT       NOT NULL,
    principal_id        VARCHAR(36)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    PRIMARY KEY (warehouse_member_id),
    CONSTRAINT uk_warehouse_member_warehouse_principal UNIQUE (warehouse_id, principal_id),
    CONSTRAINT fk_warehouse_member_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_warehouse_member_principal ON warehouse_member (principal_id);
