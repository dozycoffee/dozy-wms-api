-- WMS 스키마: 테이블 생성. 인덱스는 V2, 외래키는 V3에서 만든다.
-- 배포 전까지는 이 세 파일을 직접 고치고 개발 DB를 초기화(scripts/reset-dev-db.sh)한다.

CREATE TABLE warehouse
(
    `warehouse_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_name` varchar(100) NOT NULL,
    `address` varchar(255) NOT NULL,
    `latitude` decimal(9,6) NOT NULL,
    `longitude` decimal(9,6) NOT NULL,
    `warehouse_status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `deleted_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`warehouse_id`),
    CONSTRAINT `ck_warehouse_status` CHECK ((`warehouse_status` in ('AVAILABLE','UNAVAILABLE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE zone
(
    `zone_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `zone_code` varchar(1) NOT NULL,
    `zone_status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`zone_id`),
    CONSTRAINT `uq_zone_warehouse_code` UNIQUE (`warehouse_id`,`zone_code`),
    CONSTRAINT `ck_zone_status` CHECK ((`zone_status` in ('AVAILABLE','UNAVAILABLE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE work_area
(
    `work_area_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `area_code` varchar(50) NOT NULL,
    `used_capacity` int NOT NULL DEFAULT '0',
    `work_area_status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`work_area_id`),
    CONSTRAINT `uq_work_area_warehouse_code` UNIQUE (`warehouse_id`,`area_code`),
    CONSTRAINT `chk_work_area_used_capacity` CHECK ((`used_capacity` >= 0)),
    CONSTRAINT `ck_work_area_status` CHECK ((`work_area_status` in ('AVAILABLE','UNAVAILABLE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE location
(
    `location_id` bigint NOT NULL AUTO_INCREMENT,
    `zone_id` bigint NOT NULL,
    `location_code` varchar(10) NOT NULL,
    `max_capacity` int NOT NULL,
    `used_capacity` int NOT NULL DEFAULT '0',
    `location_status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`location_id`),
    CONSTRAINT `uq_location_zone_code` UNIQUE (`zone_id`,`location_code`),
    CONSTRAINT `chk_location_capacity` CHECK (((`used_capacity` >= 0) and (`used_capacity` <= `max_capacity`))),
    CONSTRAINT `ck_location_status` CHECK ((`location_status` in ('AVAILABLE','UNAVAILABLE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE warehouse_member
(
    `warehouse_member_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `principal_id` varchar(36) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`warehouse_member_id`),
    CONSTRAINT `uk_warehouse_member_warehouse_principal` UNIQUE (`warehouse_id`,`principal_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE warehouse_access_event
(
    `warehouse_access_event_id` bigint NOT NULL AUTO_INCREMENT,
    `principal_id` varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `event_id` varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `occurred_at` datetime(6) NOT NULL,
    `attempt_count` int NOT NULL DEFAULT '0',
    `next_attempt_at` datetime(6) NOT NULL,
    `last_error` varchar(500) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`warehouse_access_event_id`),
    CONSTRAINT `uq_warehouse_access_event_principal` UNIQUE (`principal_id`),
    CONSTRAINT `chk_warehouse_access_event_attempt_count` CHECK ((`attempt_count` >= 0))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE product
(
    `product_id` bigint NOT NULL AUTO_INCREMENT,
    `product_code` varchar(50) NOT NULL,
    `product_name` varchar(100) NOT NULL,
    `category` varchar(50) NOT NULL,
    `unit` varchar(20) NOT NULL,
    `shelf_life_days` int DEFAULT NULL,
    `product_status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `source_occurred_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`product_id`),
    CONSTRAINT `uq_product_code` UNIQUE (`product_code`),
    CONSTRAINT `chk_product_shelf_life_days` CHECK (((`shelf_life_days` is null) or (`shelf_life_days` >= 0))),
    CONSTRAINT `ck_product_category` CHECK ((`category` in ('BEAN','SYRUP','POWDER','DAIRY','SUPPLY','MD'))),
    CONSTRAINT `ck_product_status` CHECK ((`product_status` in ('ACTIVE','INACTIVE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound
(
    `inbound_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `expected_arrival_date` date NOT NULL,
    `status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`inbound_id`),
    CONSTRAINT `ck_inbound_status` CHECK ((`status` in ('EXPECTED','WAITING','PROCESSING','COMPLETED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound_item
(
    `inbound_item_id` bigint NOT NULL AUTO_INCREMENT,
    `inbound_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `zone_id` bigint NOT NULL,
    `expected_quantity` int NOT NULL,
    `expected_lot_number` varchar(50) DEFAULT NULL,
    `expected_expiration_date` date DEFAULT NULL,
    `actual_quantity` int DEFAULT NULL,
    `inspection_status` varchar(50) NOT NULL DEFAULT 'PENDING',
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`inbound_item_id`),
    CONSTRAINT `chk_inbound_item_actual_quantity` CHECK (((`actual_quantity` is null) or (`actual_quantity` >= 0))),
    CONSTRAINT `chk_inbound_item_expected_quantity` CHECK ((`expected_quantity` > 0)),
    CONSTRAINT `ck_inbound_item_inspection_status` CHECK ((`inspection_status` in ('PENDING','INSPECTED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inbound_receipt
(
    `inbound_receipt_id` bigint NOT NULL AUTO_INCREMENT,
    `inbound_item_id` bigint NOT NULL,
    `lot_number` varchar(50) NOT NULL,
    `manufacture_date` date DEFAULT NULL,
    `expiration_date` date DEFAULT NULL,
    `quantity` int NOT NULL,
    `inspection_result` varchar(50) NOT NULL,
    `defect_reason` varchar(50) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`inbound_receipt_id`),
    CONSTRAINT `chk_inbound_receipt_dates` CHECK (((`manufacture_date` is null) or (`expiration_date` is null) or (`expiration_date` >= `manufacture_date`))),
    CONSTRAINT `chk_inbound_receipt_defect_reason_result` CHECK (((`inspection_result` = 'DEFECTIVE') = (`defect_reason` is not null))),
    CONSTRAINT `chk_inbound_receipt_quantity` CHECK ((`quantity` > 0)),
    CONSTRAINT `ck_inbound_receipt_defect_reason` CHECK (((`defect_reason` is null) or (`defect_reason` in ('DAMAGED','EXPIRED','QUALITY','OTHER')))),
    CONSTRAINT `ck_inbound_receipt_inspection_result` CHECK ((`inspection_result` in ('NORMAL','DEFECTIVE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound
(
    `outbound_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `reservation_id` bigint DEFAULT NULL,
    `channel` varchar(50) DEFAULT NULL,
    `external_order_id` varchar(200) DEFAULT NULL,
    `reallocation_count` int NOT NULL DEFAULT '0',
    PRIMARY KEY (`outbound_id`),
    CONSTRAINT `uq_outbound_reservation` UNIQUE (`warehouse_id`,`reservation_id`),
    CONSTRAINT `ck_outbound_status` CHECK ((`status` in ('REQUESTED','PICKING','INSPECTING','COMPLETED','CANCEL_REQUESTED','CANCELED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound_item
(
    `outbound_item_id` bigint NOT NULL AUTO_INCREMENT,
    `outbound_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `requested_quantity` int NOT NULL,
    `picked_quantity` int DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`outbound_item_id`),
    CONSTRAINT `chk_outbound_item_picked_quantity` CHECK (((`picked_quantity` is null) or (`picked_quantity` >= 0))),
    CONSTRAINT `chk_outbound_item_requested_quantity` CHECK ((`requested_quantity` > 0))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound_allocation
(
    `outbound_allocation_id` bigint NOT NULL AUTO_INCREMENT,
    `outbound_item_id` bigint NOT NULL,
    `external_inventory_id` bigint NOT NULL,
    `lot_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `expiration_date` date DEFAULT NULL,
    `quantity` int NOT NULL,
    `picked_quantity` int DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`outbound_allocation_id`),
    CONSTRAINT `uq_outbound_allocation` UNIQUE (`outbound_item_id`,`external_inventory_id`),
    CONSTRAINT `chk_outbound_allocation_picked_quantity` CHECK (((`picked_quantity` is null) or (`picked_quantity` between 0 and `quantity`))),
    CONSTRAINT `chk_outbound_allocation_quantity` CHECK ((`quantity` > 0))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE outbound_allocation_pick
(
    `outbound_allocation_pick_id` bigint NOT NULL AUTO_INCREMENT,
    `outbound_allocation_id` bigint NOT NULL,
    `location_stock_id` bigint NOT NULL,
    `quantity` int NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`outbound_allocation_pick_id`),
    CONSTRAINT `uq_outbound_allocation_pick` UNIQUE (`outbound_allocation_id`,`location_stock_id`),
    CONSTRAINT `chk_outbound_allocation_pick_quantity` CHECK ((`quantity` > 0))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE return_request
(
    `return_request_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`return_request_id`),
    CONSTRAINT `ck_return_request_status` CHECK ((`status` in ('RECEIVED','INSPECTING','COMPLETED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE return_item
(
    `return_item_id` bigint NOT NULL AUTO_INCREMENT,
    `return_request_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `expected_quantity` int NOT NULL,
    `actual_quantity` int DEFAULT NULL,
    `inspection_result` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`return_item_id`),
    CONSTRAINT `chk_return_item_actual_quantity` CHECK (((`actual_quantity` is null) or (`actual_quantity` >= 0))),
    CONSTRAINT `chk_return_item_expected_quantity` CHECK ((`expected_quantity` > 0)),
    CONSTRAINT `ck_return_item_inspection_result` CHECK ((`inspection_result` in ('PENDING','NORMAL','DEFECTIVE')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE disposal
(
    `disposal_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `status` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `source_event_id` varchar(36) DEFAULT NULL,
    PRIMARY KEY (`disposal_id`),
    CONSTRAINT `uq_disposal_source_event` UNIQUE (`source_event_id`,`warehouse_id`),
    CONSTRAINT `ck_disposal_status` CHECK ((`status` in ('REQUESTED','APPROVED','COMPLETED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE disposal_item
(
    `disposal_item_id` bigint NOT NULL AUTO_INCREMENT,
    `disposal_id` bigint NOT NULL,
    `quantity` int NOT NULL,
    `reason` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `location_stock_id` bigint NOT NULL,
    PRIMARY KEY (`disposal_item_id`),
    CONSTRAINT `chk_disposal_item_quantity` CHECK ((`quantity` > 0)),
    CONSTRAINT `ck_disposal_item_reason` CHECK ((`reason` in ('EXPIRED','INSPECTION_DEFECT','RETURN_DEFECT','OTHER')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_audit
(
    `stock_audit_id` bigint NOT NULL AUTO_INCREMENT,
    `warehouse_id` bigint NOT NULL,
    `zone_id` bigint NOT NULL,
    `status` varchar(50) NOT NULL,
    `assignee` varchar(100) DEFAULT NULL,
    `approved_by` varchar(100) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`stock_audit_id`),
    CONSTRAINT `ck_stock_audit_status` CHECK ((`status` in ('SCHEDULED','IN_PROGRESS','COMPLETED','CLOSED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_audit_item
(
    `stock_audit_item_id` bigint NOT NULL AUTO_INCREMENT,
    `stock_audit_id` bigint NOT NULL,
    `snapshot_quantity` int NOT NULL,
    `snapshot_taken_at` datetime(6) NOT NULL,
    `counted_quantity` int DEFAULT NULL,
    `has_uncommitted_movement` tinyint(1) NOT NULL DEFAULT '0',
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `location_stock_id` bigint NOT NULL,
    PRIMARY KEY (`stock_audit_item_id`),
    CONSTRAINT `chk_stock_audit_item_counted_quantity` CHECK (((`counted_quantity` is null) or (`counted_quantity` >= 0))),
    CONSTRAINT `chk_stock_audit_item_snapshot_quantity` CHECK ((`snapshot_quantity` >= 0))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE location_stock
(
    `location_stock_id` bigint NOT NULL AUTO_INCREMENT,
    `location_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `lot_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `quality_status` varchar(50) NOT NULL,
    `quantity` int NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    `held_quantity` int NOT NULL DEFAULT '0',
    PRIMARY KEY (`location_stock_id`),
    CONSTRAINT `uq_location_stock_key` UNIQUE (`location_id`,`product_id`,`lot_number`,`quality_status`),
    CONSTRAINT `chk_location_stock_held_quantity` CHECK (((`held_quantity` >= 0) and (`held_quantity` <= `quantity`))),
    CONSTRAINT `chk_location_stock_quantity` CHECK ((`quantity` >= 0)),
    CONSTRAINT `ck_location_stock_quality_status` CHECK ((`quality_status` in ('NORMAL','DEFECTIVE','DISPOSAL_SCHEDULED')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE inventory_call
(
    `inventory_call_id` bigint NOT NULL AUTO_INCREMENT,
    `call_type` varchar(50) NOT NULL,
    `warehouse_id` bigint NOT NULL,
    `idempotency_key` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `request_path` varchar(200) NOT NULL,
    `request_body` text NOT NULL,
    `status` varchar(50) NOT NULL,
    `attempt_count` int NOT NULL DEFAULT '0',
    `next_attempt_at` datetime(6) NOT NULL,
    `last_error` varchar(500) DEFAULT NULL,
    `response_body` text,
    `sent_at` datetime(6) DEFAULT NULL,
    `dead_at` datetime(6) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`inventory_call_id`),
    CONSTRAINT `uq_inventory_call_idempotency_key` UNIQUE (`idempotency_key`),
    CONSTRAINT `chk_inventory_call_attempt_count` CHECK ((`attempt_count` >= 0)),
    CONSTRAINT `ck_inventory_call_status` CHECK ((`status` in ('PENDING','SENT','DEAD','DISCARDED'))),
    CONSTRAINT `ck_inventory_call_type` CHECK ((`call_type` in ('INBOUND_RECEIPT','RETURN','DISPOSAL','FULFILLMENT','REALLOCATION','RELEASE','STOCK_ADJUSTMENT')))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
