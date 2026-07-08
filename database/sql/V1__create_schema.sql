-- Showdown Tournament 스키마 생성 스크립트 (Flyway V1)
-- 대상 DBMS: PostgreSQL 15 이상
-- 실행 전 showdown_tournament 데이터베이스에 접속해야 한다.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE tournament_status AS ENUM ('draft', 'published', 'running', 'finished', 'archived');
CREATE TYPE division_category AS ENUM ('male', 'female', 'mixed', 'youth', 'open', 'custom');
CREATE TYPE participant_status AS ENUM ('active', 'withdrawn', 'disqualified');
CREATE TYPE stage_type AS ENUM ('round_robin', 'knockout', 'placement');
CREATE TYPE stage_status AS ENUM ('draft', 'published', 'running', 'finished');
CREATE TYPE group_type AS ENUM ('league', 'knockout', 'placement');
CREATE TYPE match_status AS ENUM ('scheduled', 'running', 'completed', 'cancelled', 'walkover');
CREATE TYPE match_end_reason AS ENUM ('normal', 'giving_up', 'default_loss', 'bye');
CREATE TYPE match_side AS ENUM ('player1', 'player2');
CREATE TYPE point_event_type AS ENUM ('goal', 'fault', 'penalty', 'correction');
CREATE TYPE assignment_source AS ENUM ('auto', 'manual', 'imported');

CREATE TABLE tournaments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    location varchar(200),
    start_date date NOT NULL,
    end_date date NOT NULL,
    timezone varchar(80) NOT NULL DEFAULT 'UTC',
    status tournament_status NOT NULL DEFAULT 'draft',
    default_language varchar(10) NOT NULL DEFAULT 'ko',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_tournaments_code UNIQUE (code),
    CONSTRAINT ck_tournaments_date_range CHECK (end_date >= start_date),
    CONSTRAINT ck_tournaments_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_tournaments_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE divisions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    code varchar(40) NOT NULL,
    category division_category NOT NULL DEFAULT 'open',
    age_min integer,
    age_max integer,
    sort_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_divisions_tournament_code UNIQUE (tournament_id, code),
    CONSTRAINT ck_divisions_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_divisions_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_divisions_age_range CHECK (
        age_min IS NULL
        OR age_max IS NULL
        OR age_max >= age_min
    )
);

CREATE TABLE players (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name varchar(200) NOT NULL,
    family_name varchar(100),
    given_name varchar(100),
    country_code char(3),
    external_ref varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_players_display_name_not_blank CHECK (length(trim(display_name)) > 0),
    CONSTRAINT uq_players_external_ref UNIQUE (external_ref)
);

CREATE TABLE tournament_players (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid NOT NULL REFERENCES divisions(id) ON DELETE CASCADE,
    player_id uuid NOT NULL REFERENCES players(id) ON DELETE RESTRICT,
    seed_no integer,
    entry_no integer,
    display_name_override varchar(200),
    club_name varchar(200),
    status participant_status NOT NULL DEFAULT 'active',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_tournament_players_player UNIQUE (tournament_id, division_id, player_id),
    CONSTRAINT uq_tournament_players_entry UNIQUE (tournament_id, division_id, entry_no),
    CONSTRAINT ck_tournament_players_seed_positive CHECK (seed_no IS NULL OR seed_no > 0),
    CONSTRAINT ck_tournament_players_entry_positive CHECK (entry_no IS NULL OR entry_no > 0)
);

CREATE TABLE stages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid NOT NULL REFERENCES divisions(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    stage_type stage_type NOT NULL,
    default_match_duration_minutes integer NOT NULL DEFAULT 30,
    sort_order integer NOT NULL DEFAULT 0,
    status stage_status NOT NULL DEFAULT 'draft',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_stages_sort UNIQUE (tournament_id, division_id, sort_order),
    CONSTRAINT ck_stages_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_stages_duration_positive CHECK (default_match_duration_minutes > 0)
);

CREATE TABLE groups (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid NOT NULL REFERENCES divisions(id) ON DELETE CASCADE,
    stage_id uuid NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    code varchar(60) NOT NULL,
    name varchar(120) NOT NULL,
    group_type group_type NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_groups_tournament_division_code UNIQUE (tournament_id, division_id, code),
    CONSTRAINT ck_groups_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_groups_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE group_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    tournament_player_id uuid NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    slot_no integer NOT NULL,
    source_rule text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_members_player UNIQUE (group_id, tournament_player_id),
    CONSTRAINT uq_group_members_slot UNIQUE (group_id, slot_no),
    CONSTRAINT ck_group_members_slot_positive CHECK (slot_no > 0)
);

CREATE TABLE courts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    name varchar(80) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_courts_tournament_name UNIQUE (tournament_id, name),
    CONSTRAINT ck_courts_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE officials (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    short_code varchar(40),
    role_name varchar(60) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_officials_tournament_short_code UNIQUE (tournament_id, short_code),
    CONSTRAINT ck_officials_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_officials_role_not_blank CHECK (length(trim(role_name)) > 0)
);

CREATE TABLE schedule_duration_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid REFERENCES divisions(id) ON DELETE CASCADE,
    stage_type stage_type NOT NULL,
    match_duration_minutes integer NOT NULL,
    break_minutes integer NOT NULL DEFAULT 0,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_schedule_duration_positive CHECK (match_duration_minutes > 0),
    CONSTRAINT ck_schedule_break_non_negative CHECK (break_minutes >= 0)
);

CREATE TABLE schedule_official_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid REFERENCES divisions(id) ON DELETE CASCADE,
    stage_type stage_type NOT NULL,
    required_official_count integer NOT NULL DEFAULT 1,
    max_official_count integer NOT NULL DEFAULT 2,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_schedule_official_required_count CHECK (required_official_count BETWEEN 1 AND 2),
    CONSTRAINT ck_schedule_official_max_count CHECK (max_official_count BETWEEN 1 AND 2),
    CONSTRAINT ck_schedule_official_count_order CHECK (max_official_count >= required_official_count)
);

CREATE TABLE schedule_blocks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid REFERENCES divisions(id) ON DELETE CASCADE,
    stage_id uuid REFERENCES stages(id) ON DELETE CASCADE,
    court_id uuid REFERENCES courts(id) ON DELETE CASCADE,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_schedule_blocks_time_range CHECK (ends_at > starts_at)
);

CREATE TABLE matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid NOT NULL REFERENCES divisions(id) ON DELETE CASCADE,
    stage_id uuid NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    group_id uuid REFERENCES groups(id) ON DELETE SET NULL,
    match_no integer NOT NULL,
    scheduled_at timestamptz,
    court_name varchar(80),
    duration_minutes integer,
    max_sets integer NOT NULL DEFAULT 3,
    court_id uuid REFERENCES courts(id) ON DELETE SET NULL,
    referee_id uuid REFERENCES officials(id) ON DELETE SET NULL,
    schedule_source assignment_source NOT NULL DEFAULT 'manual',
    schedule_locked boolean NOT NULL DEFAULT false,
    manual_note text,
    status match_status NOT NULL DEFAULT 'scheduled',
    end_reason match_end_reason NOT NULL DEFAULT 'normal',
    result_note text,
    winner_tournament_player_id uuid REFERENCES tournament_players(id) ON DELETE SET NULL,
    player1_match_points integer NOT NULL DEFAULT 0,
    player2_match_points integer NOT NULL DEFAULT 0,
    player1_sets_won integer NOT NULL DEFAULT 0,
    player2_sets_won integer NOT NULL DEFAULT 0,
    player1_total_points integer NOT NULL DEFAULT 0,
    player2_total_points integer NOT NULL DEFAULT 0,
    version integer NOT NULL DEFAULT 1,
    confirmed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_matches_tournament_match_no UNIQUE (tournament_id, match_no),
    CONSTRAINT ck_matches_match_no_positive CHECK (match_no > 0),
    CONSTRAINT ck_matches_duration_positive CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT ck_matches_max_sets CHECK (max_sets IN (1, 3, 5)),
    CONSTRAINT ck_matches_points_non_negative CHECK (
        player1_match_points >= 0
        AND player2_match_points >= 0
        AND player1_sets_won >= 0
        AND player2_sets_won >= 0
        AND player1_total_points >= 0
        AND player2_total_points >= 0
    ),
    CONSTRAINT ck_matches_version_positive CHECK (version > 0),
    CONSTRAINT ck_matches_winner_required CHECK (
        status NOT IN ('completed', 'walkover') OR winner_tournament_player_id IS NOT NULL
    ),
    CONSTRAINT ck_matches_end_reason_status CHECK (
        (status = 'walkover' AND end_reason IN ('giving_up', 'default_loss', 'bye'))
        OR (status <> 'walkover' AND end_reason = 'normal')
    )
);

CREATE TABLE match_players (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    tournament_player_id uuid REFERENCES tournament_players(id) ON DELETE RESTRICT,
    side match_side NOT NULL,
    source_slot varchar(200),
    assignment_source assignment_source NOT NULL DEFAULT 'manual',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_players_side UNIQUE (match_id, side),
    CONSTRAINT uq_match_players_player UNIQUE (match_id, tournament_player_id),
    CONSTRAINT ck_match_players_player_or_source CHECK (
        tournament_player_id IS NOT NULL OR source_slot IS NOT NULL
    )
);

CREATE TABLE match_officials (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    official_id uuid NOT NULL REFERENCES officials(id) ON DELETE RESTRICT,
    role_name varchar(60) NOT NULL DEFAULT 'referee',
    position_no integer NOT NULL,
    assignment_source assignment_source NOT NULL DEFAULT 'auto',
    assigned_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_officials_position UNIQUE (match_id, position_no),
    CONSTRAINT uq_match_officials_official UNIQUE (match_id, official_id),
    CONSTRAINT ck_match_officials_position CHECK (position_no BETWEEN 1 AND 2),
    CONSTRAINT ck_match_officials_role_not_blank CHECK (length(trim(role_name)) > 0)
);

CREATE TABLE match_sets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    set_no integer NOT NULL,
    player1_score integer NOT NULL,
    player2_score integer NOT NULL,
    winner_side match_side NOT NULL,
    is_tiebreak boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_sets_match_set UNIQUE (match_id, set_no),
    CONSTRAINT ck_match_sets_set_no_positive CHECK (set_no > 0),
    CONSTRAINT ck_match_sets_scores_non_negative CHECK (player1_score >= 0 AND player2_score >= 0),
    CONSTRAINT ck_match_sets_winner_matches_score CHECK (
        (winner_side = 'player1' AND player1_score > player2_score)
        OR (winner_side = 'player2' AND player2_score > player1_score)
    )
);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(254) NOT NULL,
    password_hash text NOT NULL,
    display_name varchar(120) NOT NULL,
    tournament_player_id uuid REFERENCES tournament_players(id) ON DELETE SET NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_users_display_name_not_blank CHECK (length(trim(display_name)) > 0),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (length(trim(password_hash)) > 0)
);

CREATE TABLE point_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    set_no integer NOT NULL,
    event_no integer NOT NULL,
    server_side match_side,
    service_no integer,
    event_type point_event_type NOT NULL,
    scoring_side match_side,
    player1_score_after integer NOT NULL,
    player2_score_after integer NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_point_events_match_set_event UNIQUE (match_id, set_no, event_no),
    CONSTRAINT ck_point_events_set_no_positive CHECK (set_no > 0),
    CONSTRAINT ck_point_events_event_no_positive CHECK (event_no > 0),
    CONSTRAINT ck_point_events_service_no CHECK (service_no IS NULL OR service_no IN (1, 2)),
    CONSTRAINT ck_point_events_scores_non_negative CHECK (
        player1_score_after >= 0 AND player2_score_after >= 0
    )
);

CREATE TABLE ranking_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid REFERENCES tournaments(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    win_points integer NOT NULL DEFAULT 1,
    loss_points integer NOT NULL DEFAULT 0,
    walkover_win_points integer NOT NULL DEFAULT 1,
    sort_priority jsonb NOT NULL DEFAULT '["match_points","wins","set_difference","point_difference","points_for","head_to_head","seed_no"]'::jsonb,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_ranking_rules_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_ranking_rules_points_non_negative CHECK (
        win_points >= 0 AND loss_points >= 0 AND walkover_win_points >= 0
    )
);

CREATE TABLE rankings_snapshots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    division_id uuid NOT NULL REFERENCES divisions(id) ON DELETE CASCADE,
    stage_id uuid REFERENCES stages(id) ON DELETE CASCADE,
    group_id uuid REFERENCES groups(id) ON DELETE CASCADE,
    tournament_player_id uuid NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    rank_no integer NOT NULL,
    matches_played integer NOT NULL DEFAULT 0,
    wins integer NOT NULL DEFAULT 0,
    losses integer NOT NULL DEFAULT 0,
    match_points integer NOT NULL DEFAULT 0,
    sets_won integer NOT NULL DEFAULT 0,
    sets_lost integer NOT NULL DEFAULT 0,
    set_difference integer NOT NULL DEFAULT 0,
    points_for integer NOT NULL DEFAULT 0,
    points_against integer NOT NULL DEFAULT 0,
    point_difference integer NOT NULL DEFAULT 0,
    tie_break_note text,
    calculated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_rankings_rank_positive CHECK (rank_no > 0),
    CONSTRAINT ck_rankings_counts_non_negative CHECK (
        matches_played >= 0
        AND wins >= 0
        AND losses >= 0
        AND match_points >= 0
        AND sets_won >= 0
        AND sets_lost >= 0
        AND points_for >= 0
        AND points_against >= 0
    )
);

CREATE TABLE roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(60) NOT NULL,
    name varchar(120) NOT NULL,
    description text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_roles_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE user_roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    tournament_id uuid REFERENCES tournaments(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid REFERENCES tournaments(id) ON DELETE SET NULL,
    user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    action varchar(120) NOT NULL,
    entity_type varchar(120) NOT NULL,
    entity_id uuid,
    before_json jsonb,
    after_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_logs_action_not_blank CHECK (length(trim(action)) > 0),
    CONSTRAINT ck_audit_logs_entity_type_not_blank CHECK (length(trim(entity_type)) > 0)
);

CREATE TABLE favorites (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id uuid NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    tournament_player_id uuid NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    user_id uuid REFERENCES users(id) ON DELETE CASCADE,
    anonymous_key varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_favorites_owner_required CHECK (user_id IS NOT NULL OR anonymous_key IS NOT NULL)
);

CREATE UNIQUE INDEX uq_ranking_rules_default_per_tournament
    ON ranking_rules (tournament_id)
    WHERE is_default = true AND tournament_id IS NOT NULL;

CREATE UNIQUE INDEX uq_favorites_user_player
    ON favorites (tournament_id, tournament_player_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX uq_favorites_anonymous_player
    ON favorites (tournament_id, tournament_player_id, anonymous_key)
    WHERE anonymous_key IS NOT NULL;

CREATE UNIQUE INDEX uq_rankings_group_player
    ON rankings_snapshots (group_id, tournament_player_id)
    WHERE group_id IS NOT NULL;

CREATE UNIQUE INDEX uq_rankings_stage_final_player
    ON rankings_snapshots (tournament_id, division_id, stage_id, tournament_player_id)
    WHERE group_id IS NULL AND stage_id IS NOT NULL;

CREATE UNIQUE INDEX uq_user_roles_global_scope
    ON user_roles (user_id, role_id)
    WHERE tournament_id IS NULL;

CREATE UNIQUE INDEX uq_user_roles_tournament_scope
    ON user_roles (user_id, role_id, tournament_id)
    WHERE tournament_id IS NOT NULL;

CREATE UNIQUE INDEX uq_schedule_duration_global_default
    ON schedule_duration_rules (stage_type)
    WHERE tournament_id IS NULL AND division_id IS NULL AND is_default = true;

CREATE UNIQUE INDEX uq_schedule_duration_tournament_default
    ON schedule_duration_rules (tournament_id, stage_type)
    WHERE tournament_id IS NOT NULL AND division_id IS NULL AND is_default = true;

CREATE UNIQUE INDEX uq_schedule_duration_division_default
    ON schedule_duration_rules (tournament_id, division_id, stage_type)
    WHERE tournament_id IS NOT NULL AND division_id IS NOT NULL AND is_default = true;

CREATE UNIQUE INDEX uq_schedule_official_global_default
    ON schedule_official_rules (stage_type)
    WHERE tournament_id IS NULL AND division_id IS NULL AND is_default = true;

CREATE UNIQUE INDEX uq_schedule_official_tournament_default
    ON schedule_official_rules (tournament_id, stage_type)
    WHERE tournament_id IS NOT NULL AND division_id IS NULL AND is_default = true;

CREATE UNIQUE INDEX uq_schedule_official_division_default
    ON schedule_official_rules (tournament_id, division_id, stage_type)
    WHERE tournament_id IS NOT NULL AND division_id IS NOT NULL AND is_default = true;

CREATE INDEX idx_tournaments_status ON tournaments (status);
CREATE INDEX idx_tournaments_start_date ON tournaments (start_date);

CREATE INDEX idx_divisions_tournament_sort ON divisions (tournament_id, sort_order);
CREATE INDEX idx_divisions_tournament_category ON divisions (tournament_id, category);

CREATE INDEX idx_players_display_name ON players (display_name);
CREATE INDEX idx_players_country_code ON players (country_code);

CREATE INDEX idx_tournament_players_list ON tournament_players (tournament_id, division_id, entry_no);
CREATE INDEX idx_tournament_players_player ON tournament_players (player_id);
CREATE INDEX idx_tournament_players_status ON tournament_players (tournament_id, status);

CREATE INDEX idx_stages_tournament_division_sort ON stages (tournament_id, division_id, sort_order);

CREATE INDEX idx_groups_tournament_division_stage_sort ON groups (tournament_id, division_id, stage_id, sort_order);

CREATE INDEX idx_group_members_group ON group_members (group_id, slot_no);
CREATE INDEX idx_group_members_player ON group_members (tournament_player_id);

CREATE INDEX idx_courts_tournament_sort ON courts (tournament_id, sort_order);

CREATE INDEX idx_officials_tournament_role ON officials (tournament_id, role_name);

CREATE INDEX idx_schedule_duration_lookup ON schedule_duration_rules (tournament_id, division_id, stage_type, is_default);
CREATE INDEX idx_schedule_official_lookup ON schedule_official_rules (tournament_id, division_id, stage_type, is_default);
CREATE INDEX idx_schedule_blocks_lookup ON schedule_blocks (tournament_id, division_id, stage_id, court_id, starts_at);
CREATE INDEX idx_schedule_blocks_active_time ON schedule_blocks (tournament_id, is_active, starts_at, ends_at);

CREATE INDEX idx_matches_schedule ON matches (tournament_id, scheduled_at, match_no);
CREATE INDEX idx_matches_status_schedule ON matches (tournament_id, status, scheduled_at);
CREATE INDEX idx_matches_group ON matches (group_id, match_no);
CREATE INDEX idx_matches_court_schedule ON matches (court_id, scheduled_at);
CREATE INDEX idx_matches_winner ON matches (winner_tournament_player_id);
CREATE INDEX idx_matches_schedule_source ON matches (tournament_id, schedule_source);

CREATE INDEX idx_match_players_match ON match_players (match_id);
CREATE INDEX idx_match_players_tournament_player ON match_players (tournament_player_id);

CREATE INDEX idx_match_officials_match ON match_officials (match_id, position_no);
CREATE INDEX idx_match_officials_official ON match_officials (official_id);

CREATE INDEX idx_match_sets_match_set ON match_sets (match_id, set_no);

CREATE INDEX idx_point_events_match_order ON point_events (match_id, set_no, event_no);
CREATE INDEX idx_point_events_created_at ON point_events (created_at);

CREATE INDEX idx_ranking_rules_tournament_default ON ranking_rules (tournament_id, is_default);

CREATE INDEX idx_rankings_public_order ON rankings_snapshots (tournament_id, division_id, stage_id, group_id, rank_no);
CREATE INDEX idx_rankings_player ON rankings_snapshots (tournament_player_id);
CREATE INDEX idx_rankings_calculated_at ON rankings_snapshots (calculated_at);

CREATE INDEX idx_users_active ON users (is_active);

CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_tournament ON user_roles (tournament_id);

CREATE INDEX idx_audit_logs_tournament_created ON audit_logs (tournament_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);

CREATE INDEX idx_favorites_user ON favorites (user_id);
CREATE INDEX idx_favorites_anonymous ON favorites (anonymous_key);
CREATE INDEX idx_favorites_player ON favorites (tournament_player_id);

COMMIT;
