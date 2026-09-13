CREATE TABLE product
(
    product_id      BIGINT       NOT NULL AUTO_INCREMENT,
    product_code    VARCHAR(50)  NOT NULL,
    product_name    VARCHAR(100) NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    unit            VARCHAR(20)  NOT NULL,
    shelf_life_days INT          NULL,
    product_status  VARCHAR(50)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    deleted_by      VARCHAR(100) NULL,
    PRIMARY KEY (product_id),
    CONSTRAINT uq_product_code UNIQUE (product_code),
    CONSTRAINT fk_product_category FOREIGN KEY (category) REFERENCES common_code (code),
    CONSTRAINT fk_product_status FOREIGN KEY (product_status) REFERENCES common_code (code),
    CONSTRAINT chk_product_shelf_life_days CHECK (shelf_life_days IS NULL OR shelf_life_days >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
