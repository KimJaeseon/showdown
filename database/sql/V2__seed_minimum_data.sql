-- Showdown Tournament 최소 기본 데이터 입력 스크립트 (Flyway V2)
-- 실제 대회, 선수, 경기 샘플 데이터는 넣지 않는다.

BEGIN;

INSERT INTO roles (code, name, description)
VALUES
    ('system_admin', 'System administrator', '시스템 전체를 관리할 수 있는 권한'),
    ('tournament_admin', 'Tournament administrator', '특정 대회를 관리할 수 있는 권한'),
    ('scorer', 'Scorer', '경기 점수와 결과를 입력할 수 있는 권한'),
    ('player', 'Player', '본인의 경기 일정과 결과를 조회할 수 있는 권한')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO ranking_rules (
    tournament_id,
    name,
    win_points,
    loss_points,
    walkover_win_points,
    sort_priority,
    is_default
)
SELECT
    NULL,
    'Default Showdown ranking rule',
    1,
    0,
    1,
    '["match_points","wins","set_difference","point_difference","points_for","head_to_head","seed_no"]'::jsonb,
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM ranking_rules
    WHERE tournament_id IS NULL
      AND name = 'Default Showdown ranking rule'
);

INSERT INTO schedule_duration_rules (
    tournament_id,
    division_id,
    stage_type,
    match_duration_minutes,
    break_minutes,
    is_default
)
VALUES
    (NULL, NULL, 'round_robin', 30, 0, true),
    (NULL, NULL, 'knockout', 45, 0, true),
    (NULL, NULL, 'placement', 45, 0, true)
ON CONFLICT DO NOTHING;

INSERT INTO schedule_official_rules (
    tournament_id,
    division_id,
    stage_type,
    required_official_count,
    max_official_count,
    is_default
)
VALUES
    (NULL, NULL, 'round_robin', 1, 2, true),
    (NULL, NULL, 'knockout', 1, 2, true),
    (NULL, NULL, 'placement', 1, 2, true)
ON CONFLICT DO NOTHING;

COMMIT;
