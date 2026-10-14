-- 조회용 인덱스. 유니크 제약은 V1의 테이블 정의에, 외래키가 쓰는 인덱스는 V3에서 외래키와 함께 만들어진다.

CREATE INDEX `idx_warehouse_member_principal` ON warehouse_member (`principal_id`);
CREATE INDEX `idx_warehouse_access_event_next_attempt` ON warehouse_access_event (`next_attempt_at`);
CREATE INDEX `idx_inbound_receipt_inbound_item` ON inbound_receipt (`inbound_item_id`);
CREATE INDEX `idx_location_stock_product_lot_quality` ON location_stock (`product_id`,`lot_number`,`quality_status`);
CREATE INDEX `idx_inventory_call_dispatch` ON inventory_call (`warehouse_id`,`status`,`inventory_call_id`);
CREATE INDEX `idx_inventory_call_status_created` ON inventory_call (`status`,`inventory_call_id`);
