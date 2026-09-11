CREATE TABLE warehouse
(
    warehouse_id   BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_name VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    latitude       DECIMAL(9, 6) NOT NULL,
    longitude      DECIMAL(9, 6) NOT NULL,
    warehouse_status VARCHAR(50) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    deleted_at     DATETIME(6)  NULL,
    deleted_by     VARCHAR(100) NULL,
    PRIMARY KEY (warehouse_id),
    CONSTRAINT fk_warehouse_status FOREIGN KEY (warehouse_status) REFERENCES common_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE zone
(
    zone_id      BIGINT      NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT      NOT NULL,
    zone_code    VARCHAR(1)  NOT NULL,
    zone_status  VARCHAR(50) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    PRIMARY KEY (zone_id),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_zone_status FOREIGN KEY (zone_status) REFERENCES common_code (code),
    CONSTRAINT uq_zone_warehouse_code UNIQUE (warehouse_id, zone_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE work_area
(
    work_area_id     BIGINT      NOT NULL AUTO_INCREMENT,
    warehouse_id     BIGINT      NOT NULL,
    area_code        VARCHAR(50) NOT NULL,
    used_capacity    INT         NOT NULL DEFAULT 0,
    work_area_status VARCHAR(50) NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    PRIMARY KEY (work_area_id),
    CONSTRAINT fk_work_area_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    CONSTRAINT fk_work_area_status FOREIGN KEY (work_area_status) REFERENCES common_code (code),
    CONSTRAINT uq_work_area_warehouse_code UNIQUE (warehouse_id, area_code),
    CONSTRAINT chk_work_area_used_capacity CHECK (used_capacity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE location
(
    location_id     BIGINT      NOT NULL AUTO_INCREMENT,
    zone_id         BIGINT      NOT NULL,
    location_code   VARCHAR(10) NOT NULL,
    max_capacity    INT         NOT NULL,
    used_capacity   INT         NOT NULL DEFAULT 0,
    location_status VARCHAR(50) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    PRIMARY KEY (location_id),
    CONSTRAINT fk_location_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id),
    CONSTRAINT fk_location_status FOREIGN KEY (location_status) REFERENCES common_code (code),
    CONSTRAINT uq_location_zone_code UNIQUE (zone_id, location_code),
    CONSTRAINT chk_location_capacity CHECK (used_capacity >= 0 AND used_capacity <= max_capacity)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
