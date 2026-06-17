-- Showdown Tournament 데이터베이스 생성 스크립트
-- 대상 DBMS: PostgreSQL 15 이상
-- 실행 예시:
-- psql -U postgres -f sql/00_create_database.sql
--
-- PostgreSQL은 CREATE DATABASE IF NOT EXISTS를 직접 지원하지 않는다.
-- 아래 스크립트는 psql의 \gexec 기능을 사용하여 데이터베이스가 없을 때만 생성한다.

SELECT 'CREATE DATABASE showdown_tournament
    WITH
    ENCODING = ''UTF8''
    LC_COLLATE = ''C''
    LC_CTYPE = ''C''
    TEMPLATE = template0'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'showdown_tournament'
)\gexec

-- 데이터베이스 생성 후 다음 명령으로 접속한다.
-- \c showdown_tournament
--
-- pgcrypto 확장은 01_create_schema.sql에서 생성한다.
