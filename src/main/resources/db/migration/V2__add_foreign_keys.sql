-- 테이블 간 외래키 연결. 테이블 정의는 V1을 참고한다.

ALTER TABLE zone
    ADD CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE work_area
    ADD CONSTRAINT fk_work_area_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE location
    ADD CONSTRAINT fk_location_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id);

ALTER TABLE lot
    ADD CONSTRAINT fk_lot_product FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) REFERENCES location (location_id),
    ADD CONSTRAINT fk_inventory_lot FOREIGN KEY (lot_id) REFERENCES lot (lot_id),
    ADD CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE allocation
    ADD CONSTRAINT fk_allocation_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id);

ALTER TABLE inbound
    ADD CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE inbound_item
    ADD CONSTRAINT fk_inbound_item_inbound FOREIGN KEY (inbound_id) REFERENCES inbound (inbound_id),
    ADD CONSTRAINT fk_inbound_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    ADD CONSTRAINT fk_inbound_item_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id);

ALTER TABLE outbound
    ADD CONSTRAINT fk_outbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE outbound_item
    ADD CONSTRAINT fk_outbound_item_outbound FOREIGN KEY (outbound_id) REFERENCES outbound (outbound_id),
    ADD CONSTRAINT fk_outbound_item_product FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE return_request
    ADD CONSTRAINT fk_return_request_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE return_item
    ADD CONSTRAINT fk_return_item_product FOREIGN KEY (product_id) REFERENCES product (product_id),
    ADD CONSTRAINT fk_return_item_return_request FOREIGN KEY (return_request_id) REFERENCES return_request (return_request_id);

ALTER TABLE disposal
    ADD CONSTRAINT fk_disposal_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);

ALTER TABLE disposal_item
    ADD CONSTRAINT fk_disposal_item_disposal FOREIGN KEY (disposal_id) REFERENCES disposal (disposal_id),
    ADD CONSTRAINT fk_disposal_item_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id);

ALTER TABLE inventory_history
    ADD CONSTRAINT fk_inventory_history_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id);

ALTER TABLE stock_audit
    ADD CONSTRAINT fk_stock_audit_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
    ADD CONSTRAINT fk_stock_audit_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id);

ALTER TABLE stock_audit_item
    ADD CONSTRAINT fk_stock_audit_item_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id),
    ADD CONSTRAINT fk_stock_audit_item_stock_audit FOREIGN KEY (stock_audit_id) REFERENCES stock_audit (stock_audit_id);

ALTER TABLE warehouse_member
    ADD CONSTRAINT fk_warehouse_member_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id);
