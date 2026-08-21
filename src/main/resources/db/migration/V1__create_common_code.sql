CREATE TABLE common_code
(
    code        VARCHAR(50)  NOT NULL,
    group_code  VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,
    PRIMARY KEY (code),
    INDEX idx_common_code_group_code (group_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
