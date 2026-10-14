-- 피킹 불가 Lot 재할당 요청의 회차. inventory 멱등 키(wms-reallocation-{출고 ID}-{회차})에 쓰고, 로컬 반영이 끝난 뒤에만 올린다.
ALTER TABLE outbound
    ADD COLUMN reallocation_count INT NOT NULL DEFAULT 0;
