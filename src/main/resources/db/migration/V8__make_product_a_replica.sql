-- 상품 마스터는 inventory 서비스가 소유하고 WMS는 이벤트로 받은 사본만 가진다(ADR-0021).
-- 사본에는 soft delete가 없고, 마지막으로 반영한 이벤트의 발생 시각을 남겨 오래된 이벤트가 최신 값을 덮지 못하게 한다.
ALTER TABLE product
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by,
    ADD COLUMN source_occurred_at DATETIME(6) DEFAULT NULL;
