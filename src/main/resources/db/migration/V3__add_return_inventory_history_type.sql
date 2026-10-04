ALTER TABLE inventory_history DROP CHECK ck_inventory_history_type;

ALTER TABLE inventory_history
    ADD CONSTRAINT ck_inventory_history_type
        CHECK (history_type IN ('INBOUND', 'RETURN', 'OUTBOUND', 'DISPOSAL', 'ADJUSTMENT'));
