-- 컷오버 1단계(ADR-0024): 폐기·실사·출고가 레거시 Inventory 대신 Location 장부를 가리킨다.
-- 개발 단계라 데이터를 이관하지 않고, 레거시 재고 행을 가리키던 문서 행은 지운다(ADR-0017 전제).

-- 출고 피킹 점유: 가용 수량 = quantity - held_quantity
ALTER TABLE location_stock
    ADD COLUMN held_quantity INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_location_stock_held_quantity CHECK ((held_quantity >= 0) AND (held_quantity <= quantity));

DELETE FROM disposal_item;
DELETE FROM disposal;
DELETE FROM stock_audit_item;
DELETE FROM stock_audit;

ALTER TABLE disposal_item DROP FOREIGN KEY fk_disposal_item_inventory;
ALTER TABLE disposal_item DROP COLUMN inventory_id;
ALTER TABLE disposal_item
    ADD COLUMN location_stock_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_disposal_item_location_stock FOREIGN KEY (location_stock_id) REFERENCES location_stock (location_stock_id);

ALTER TABLE stock_audit_item DROP FOREIGN KEY fk_stock_audit_item_inventory;
ALTER TABLE stock_audit_item DROP COLUMN inventory_id;
ALTER TABLE stock_audit_item
    ADD COLUMN location_stock_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_stock_audit_item_location_stock FOREIGN KEY (location_stock_id) REFERENCES location_stock (location_stock_id);

-- LOT_EXPIRED 이벤트가 만든 폐기 신청. 같은 이벤트를 두 번 처리하지 않도록 이벤트 ID와 창고로 유일하게 한다.
ALTER TABLE disposal
    ADD COLUMN source_event_id VARCHAR(36) DEFAULT NULL,
    ADD CONSTRAINT uq_disposal_source_event UNIQUE (source_event_id, warehouse_id);

-- 출고 피킹 계획: 확정 예약의 Lot 할당을 어느 Location 장부 행에서 몇 개 꺼낼지
CREATE TABLE outbound_allocation_pick
(
    outbound_allocation_pick_id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_allocation_id BIGINT NOT NULL,
    location_stock_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (outbound_allocation_pick_id),
    CONSTRAINT uq_outbound_allocation_pick UNIQUE (outbound_allocation_id, location_stock_id),
    CONSTRAINT fk_outbound_allocation_pick_allocation FOREIGN KEY (outbound_allocation_id) REFERENCES outbound_allocation (outbound_allocation_id),
    CONSTRAINT fk_outbound_allocation_pick_location_stock FOREIGN KEY (location_stock_id) REFERENCES location_stock (location_stock_id),
    CONSTRAINT chk_outbound_allocation_pick_quantity CHECK (quantity > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
