-- 출고 취소: 피킹 이후 취소 요청은 관리자 승인(원위치 확인)을 기다린다(CANCEL_REQUESTED).
ALTER TABLE outbound DROP CHECK ck_outbound_status;

ALTER TABLE outbound
    ADD CONSTRAINT ck_outbound_status
        CHECK (status IN ('REQUESTED', 'PICKING', 'INSPECTING', 'COMPLETED', 'CANCEL_REQUESTED', 'CANCELED'));
