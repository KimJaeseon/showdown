-- Showdown Tournament 토너먼트 다라운드 승자 연결 컬럼 추가 스크립트 (Flyway V4)
-- 아직 선수가 정해지지 않은 다음 라운드 경기 슬롯이 이전 라운드 경기의 승자를 자동으로
-- 이어받을 수 있도록 matches에 자기 참조 컬럼을 추가한다.

BEGIN;

ALTER TABLE matches
    ADD COLUMN player1_source_match_id uuid REFERENCES matches(id) ON DELETE SET NULL,
    ADD COLUMN player2_source_match_id uuid REFERENCES matches(id) ON DELETE SET NULL;

COMMIT;
