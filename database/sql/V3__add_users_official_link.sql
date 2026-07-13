-- Showdown Tournament 사용자-심판 연결 컬럼 추가 스크립트 (Flyway V3)
-- 기록원(scorer) 계정이 자신에게 배정된 경기만 채점할 수 있도록 users.official_id를 추가한다.

BEGIN;

ALTER TABLE users
    ADD COLUMN official_id uuid REFERENCES officials(id) ON DELETE SET NULL;

COMMIT;
