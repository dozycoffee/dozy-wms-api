-- 출고는 inventory의 예약 확정 이벤트로 만들어진다(ADR-0017 결정 3). 이전 수동 출고 행은 예약 정보가 없다.
ALTER TABLE outbound
    ADD COLUMN reservation_id BIGINT DEFAULT NULL,
    ADD COLUMN channel VARCHAR(50) DEFAULT NULL,
    ADD COLUMN external_order_id VARCHAR(200) DEFAULT NULL,
    ADD CONSTRAINT uq_outbound_reservation UNIQUE (warehouse_id, reservation_id);

-- 확정된 예약이 상품별로 할당한 Lot 단위 수량. external_inventory_id는 inventory 서비스의 재고 행 ID로 출고 확정 요청에 그대로 쓴다.
CREATE TABLE outbound_allocation
(
    outbound_allocation_id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_item_id BIGINT NOT NULL,
    external_inventory_id BIGINT NOT NULL,
    lot_number VARCHAR(50) COLLATE utf8mb4_bin NOT NULL,
    expiration_date DATE DEFAULT NULL,
    quantity INT NOT NULL,
    picked_quantity INT DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_allocation_id),
    CONSTRAINT uq_outbound_allocation UNIQUE (outbound_item_id, external_inventory_id),
    CONSTRAINT fk_outbound_allocation_item FOREIGN KEY (outbound_item_id) REFERENCES outbound_item (outbound_item_id),
    CONSTRAINT chk_outbound_allocation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_outbound_allocation_picked_quantity CHECK ((picked_quantity IS NULL) OR (picked_quantity BETWEEN 0 AND quantity))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
