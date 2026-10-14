-- 외래키 제약조건.

ALTER TABLE zone
    ADD CONSTRAINT `fk_zone_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE work_area
    ADD CONSTRAINT `fk_work_area_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE location
    ADD CONSTRAINT `fk_location_zone` FOREIGN KEY (`zone_id`) REFERENCES `zone` (`zone_id`);

ALTER TABLE warehouse_member
    ADD CONSTRAINT `fk_warehouse_member_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE inbound
    ADD CONSTRAINT `fk_inbound_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE inbound_item
    ADD CONSTRAINT `fk_inbound_item_inbound` FOREIGN KEY (`inbound_id`) REFERENCES `inbound` (`inbound_id`),
    ADD CONSTRAINT `fk_inbound_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`),
    ADD CONSTRAINT `fk_inbound_item_zone` FOREIGN KEY (`zone_id`) REFERENCES `zone` (`zone_id`);

ALTER TABLE inbound_receipt
    ADD CONSTRAINT `fk_inbound_receipt_inbound_item` FOREIGN KEY (`inbound_item_id`) REFERENCES `inbound_item` (`inbound_item_id`);

ALTER TABLE outbound
    ADD CONSTRAINT `fk_outbound_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE outbound_item
    ADD CONSTRAINT `fk_outbound_item_outbound` FOREIGN KEY (`outbound_id`) REFERENCES `outbound` (`outbound_id`),
    ADD CONSTRAINT `fk_outbound_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);

ALTER TABLE outbound_allocation
    ADD CONSTRAINT `fk_outbound_allocation_item` FOREIGN KEY (`outbound_item_id`) REFERENCES `outbound_item` (`outbound_item_id`);

ALTER TABLE outbound_allocation_pick
    ADD CONSTRAINT `fk_outbound_allocation_pick_allocation` FOREIGN KEY (`outbound_allocation_id`) REFERENCES `outbound_allocation` (`outbound_allocation_id`),
    ADD CONSTRAINT `fk_outbound_allocation_pick_location_stock` FOREIGN KEY (`location_stock_id`) REFERENCES `location_stock` (`location_stock_id`);

ALTER TABLE return_request
    ADD CONSTRAINT `fk_return_request_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE return_item
    ADD CONSTRAINT `fk_return_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`),
    ADD CONSTRAINT `fk_return_item_return_request` FOREIGN KEY (`return_request_id`) REFERENCES `return_request` (`return_request_id`);

ALTER TABLE disposal
    ADD CONSTRAINT `fk_disposal_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);

ALTER TABLE disposal_item
    ADD CONSTRAINT `fk_disposal_item_disposal` FOREIGN KEY (`disposal_id`) REFERENCES `disposal` (`disposal_id`),
    ADD CONSTRAINT `fk_disposal_item_location_stock` FOREIGN KEY (`location_stock_id`) REFERENCES `location_stock` (`location_stock_id`);

ALTER TABLE stock_audit
    ADD CONSTRAINT `fk_stock_audit_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
    ADD CONSTRAINT `fk_stock_audit_zone` FOREIGN KEY (`zone_id`) REFERENCES `zone` (`zone_id`);

ALTER TABLE stock_audit_item
    ADD CONSTRAINT `fk_stock_audit_item_location_stock` FOREIGN KEY (`location_stock_id`) REFERENCES `location_stock` (`location_stock_id`),
    ADD CONSTRAINT `fk_stock_audit_item_stock_audit` FOREIGN KEY (`stock_audit_id`) REFERENCES `stock_audit` (`stock_audit_id`);

ALTER TABLE location_stock
    ADD CONSTRAINT `fk_location_stock_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
    ADD CONSTRAINT `fk_location_stock_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);

ALTER TABLE inventory_call
    ADD CONSTRAINT `fk_inventory_call_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`);
