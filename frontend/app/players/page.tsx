"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getPlayerPortal, type BasicAuthSession } from "@/lib/api";
import { clearAuthSession, readAuthSession } from "@/lib/auth";
import { formatMatchTime, formatSets, matchEndReasonLabel, matchStatusLabel } from "@/lib/helpers";
import type { Match, PlayerPortal } from "@/lib/types";

export default function PlayerPortalPage() {
  const [auth] = useState<BasicAuthSession | null>(() => readAuthSession());
  const [portal, setPortal] = useState<PlayerPortal | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(() => Boolean(readAuthSession()));

  useEffect(() => {
    if (!auth) return;
    const session = auth;
    async function load() {
      setIsLoading(true);
      setError(null);
      try {
        setPortal(await getPlayerPortal(session));
      } catch (requestError) {
        setError(requestError instanceof Error ? requestError.message : "선수 포털 정보를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    }
    void load();
  }, [auth]);

  if (!auth) {
    return (
      <main className="container">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">선수 포털</p>
          <h1 id="page-title">로그인이 필요합니다</h1>
          <p className="lead">본인 경기 일정과 결과를 보려면 선수 계정으로 로그인하세요.</p>
          <Link className="button" href="/admin/login">로그인으로 이동</Link>
        </section>
      </main>
    );
  }

  if (isLoading) {
    return (
      <main className="container">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">선수 포털</p>
          <h1 id="page-title">불러오는 중</h1>
          <p className="lead">본인 경기 일정을 확인하고 있습니다.</p>
        </section>
      </main>
    );
  }

  if (error || !portal) {
    return (
      <main className="container">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">선수 포털</p>
          <h1 id="page-title">선수 정보를 찾을 수 없습니다</h1>
          <p className="lead">{error ?? "계정에 연결된 참가 선수가 없습니다."}</p>
          <button
            type="button"
            className="secondary"
            onClick={() => {
              clearAuthSession();
              window.location.href = "/admin/login";
            }}
          >
            다시 로그인
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="container">
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">선수 포털</p>
        <h1 id="page-title">내 경기 일정</h1>
        <p className="lead">
          참가 번호 {portal.player.entryNo || "미정"}번. 본인에게 연결된 경기만 표시합니다.
        </p>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            clearAuthSession();
            window.location.href = "/admin/login";
          }}
        >
          로그아웃
        </button>
      </section>

      <section className="panel" aria-labelledby="next-match-title">
        <h2 id="next-match-title">다음 경기</h2>
        {portal.nextMatch ? <MatchSummary match={portal.nextMatch} /> : <p>예정된 다음 경기가 없습니다.</p>}
      </section>

      <section className="grid" aria-label="개인 성적 요약">
        <StatCard title="경기" value={`${portal.stats.matchesPlayed}경기`} />
        <StatCard title="승패" value={`${portal.stats.wins}승 ${portal.stats.losses}패`} />
        <StatCard title="세트" value={`${portal.stats.setsWon}:${portal.stats.setsLost}`} />
        <StatCard title="득실" value={`${portal.stats.pointsFor}:${portal.stats.pointsAgainst}`} />
      </section>

      <section aria-labelledby="scheduled-title">
        <h2 id="scheduled-title">예정 경기</h2>
        <MatchList matches={portal.scheduledMatches} emptyText="예정 경기가 없습니다." />
      </section>

      <section aria-labelledby="completed-title">
        <h2 id="completed-title">완료 경기</h2>
        <MatchList matches={portal.completedMatches} emptyText="완료된 경기가 없습니다." />
      </section>
    </main>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <article className="panel">
      <h3>{title}</h3>
      <p>{value}</p>
    </article>
  );
}

function MatchList({ matches, emptyText }: { matches: Match[]; emptyText: string }) {
  if (matches.length === 0) return <p>{emptyText}</p>;
  return (
    <ol className="sentence-list">
      {matches.map((match) => (
        <li key={match.id}>
          <MatchSummary match={match} />
        </li>
      ))}
    </ol>
  );
}

function MatchSummary({ match }: { match: Match }) {
  return (
    <div>
      <strong>경기 {match.matchNo}번</strong>
      <p>
        {formatMatchTime(match.scheduledAt)} · {match.courtName} · {matchStatusLabel(match.status)}
        {match.endReason !== "normal" ? ` · ${matchEndReasonLabel(match.endReason)}` : ""}
      </p>
      <p>세트 점수: {formatSets(match.sets)}</p>
      {match.resultNote ? <p>메모: {match.resultNote}</p> : null}
    </div>
  );
}
