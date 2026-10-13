CREATE TABLE warehouse_access_event
(
    warehouse_access_event_id BIGINT NOT NULL AUTO_INCREMENT,
    principal_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (warehouse_access_event_id),
    CONSTRAINT uq_warehouse_access_event_principal UNIQUE (principal_id),
    INDEX idx_warehouse_access_event_next_attempt (next_attempt_at),
    CONSTRAINT chk_warehouse_access_event_attempt_count CHECK (attempt_count >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
