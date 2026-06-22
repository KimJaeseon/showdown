"use client";

import { useEffect, useRef, useState } from "react";
import { AdminFrame } from "@/components/AdminFrame";
import { EmptyState, ErrorState } from "@/components/StateViews";
import { getAdminDataset, saveMatchSets, type BasicAuthSession } from "@/lib/api";
import { clearAuthSession, readAuthSession } from "@/lib/auth";
import {
  calculateScorePreview,
  formatMatchTime,
  getDivisionName,
  getGroupName,
  getPlayerName,
  matchStatusLabel,
} from "@/lib/helpers";
import type { Match, ScoreSetInput, TournamentDataset } from "@/lib/types";

const initialSets: ScoreSetInput[] = [
  { setNo: 1, player1Score: 0, player2Score: 0 },
  { setNo: 2, player1Score: 0, player2Score: 0 },
  { setNo: 3, player1Score: 0, player2Score: 0 },
];

export function ScoringWorkspace() {
  const [auth] = useState<BasicAuthSession | null>(() => readAuthSession());
  const [dataset, setDataset] = useState<TournamentDataset | null>(null);
  const [selectedMatchId, setSelectedMatchId] = useState<string | undefined>();
  const [sets, setSets] = useState<ScoreSetInput[]>(initialSets);
  const [errors, setErrors] = useState<string[]>([]);
  const [liveMessage, setLiveMessage] = useState(() =>
    readAuthSession() ? "점수 입력 데이터를 불러오는 중입니다." : "로그인이 필요합니다.",
  );
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(() => Boolean(readAuthSession()));
  const [loadError, setLoadError] = useState<string | null>(null);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (!auth) return;
    const session = auth;

    async function loadDataset() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const nextDataset = await getAdminDataset(session);
        const firstMatch = nextDataset.matches.find((match) => match.status !== "completed") ?? nextDataset.matches[0];
        setDataset(nextDataset);
        setSelectedMatchId(firstMatch?.id);
        setSets(firstMatch ? deriveInitialSets(firstMatch) : initialSets);
        setLiveMessage(firstMatch ? "점수 입력 화면이 준비되었습니다." : "입력할 경기가 없습니다.");
      } catch (requestError) {
        setLoadError(requestError instanceof Error ? requestError.message : "점수 입력 데이터를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    }

    void loadDataset();
  }, [auth]);

  if (!auth) {
    return (
      <AdminFrame tournamentCode="seoul-open-2026" currentPath="/admin/scoring">
        <EmptyState
          title="기록원 로그인이 필요합니다"
          description="점수 저장 API는 심판 또는 관리자 인증 정보가 있어야 호출할 수 있습니다."
          actionHref="/admin/login"
          actionLabel="로그인으로 이동"
        />
      </AdminFrame>
    );
  }

  const tournamentCode = dataset?.tournament.code || "seoul-open-2026";

  if (isLoading) {
    return (
      <AdminFrame tournamentCode={tournamentCode} currentPath="/admin/scoring">
        <EmptyState title="불러오는 중" description="백엔드 API에서 경기 목록을 가져오고 있습니다." />
      </AdminFrame>
    );
  }

  if (loadError) {
    return (
      <AdminFrame tournamentCode={tournamentCode} currentPath="/admin/scoring">
        <ErrorState title="점수 입력 API 오류" message={loadError} />
      </AdminFrame>
    );
  }

  if (!dataset || dataset.matches.length === 0) {
    return (
      <AdminFrame tournamentCode={tournamentCode} currentPath="/admin/scoring">
        <EmptyState title="입력할 경기 없음" description="관리자 일정 관리에서 경기를 생성하면 점수를 입력할 수 있습니다." />
      </AdminFrame>
    );
  }

  const activeAuth = auth;
  const activeDataset = dataset;
  const selectedMatch = activeDataset.matches.find((match) => match.id === selectedMatchId) ?? activeDataset.matches[0];
  const player1Name = getPlayerName(
    selectedMatch.player1TournamentPlayerId,
    dataset.tournamentPlayers,
    dataset.players,
  );
  const player2Name = getPlayerName(
    selectedMatch.player2TournamentPlayerId,
    dataset.tournamentPlayers,
    dataset.players,
  );

  const preview = calculateScorePreview(sets, player1Name, player2Name);

  function selectMatch(match: Match) {
    setSelectedMatchId(match.id);
    setSets(deriveInitialSets(match));
    setErrors([]);
    setLiveMessage(`경기 ${match.matchNo}번을 선택했습니다.`);
    window.setTimeout(() => formRef.current?.focus(), 0);
  }

  function updateSetScore(index: number, field: "player1Score" | "player2Score", value: string) {
    const parsed = value === "" ? NaN : Number(value);
    setSets((current) =>
      current.map((set, setIndex) => (setIndex === index ? { ...set, [field]: parsed } : set)),
    );
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = validateSets(sets);
    setErrors(validationErrors);

    if (validationErrors.length > 0) {
      setLiveMessage(`저장할 수 없습니다. 오류 ${validationErrors.length}개를 확인하세요.`);
      return;
    }

    setIsSaving(true);
    try {
      const saved = await saveMatchSets(activeAuth, selectedMatch.id, sets, selectedMatch.version);
      setDataset({
        ...activeDataset,
        matches: activeDataset.matches.map((match) => (match.id === saved.id ? saved : match)),
      });
      setSelectedMatchId(saved.id);
      setSets(deriveInitialSets(saved));
      setLiveMessage(`경기 ${selectedMatch.matchNo}번 점수를 저장했습니다.`);
    } catch (requestError) {
      setLiveMessage("점수를 저장하지 못했습니다. 오류 안내를 확인하세요.");
      setErrors([requestError instanceof Error ? requestError.message : "점수 저장 요청에 실패했습니다."]);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <AdminFrame tournamentCode={tournamentCode} currentPath="/admin/scoring">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">기록원 화면</p>
          <h1 id="page-title">세트 점수 입력</h1>
          <p className="lead">
            경기를 선택하고 세트별 점수를 입력합니다. 저장 상태와 오류는 화면 리더가 읽을 수 있는
            알림 영역으로 전달됩니다.
          </p>
          <button
            className="secondary"
            type="button"
            onClick={() => {
              clearAuthSession();
              window.location.href = "/admin/login";
            }}
          >
            로그아웃
          </button>
        </section>

        <div className="score-layout">
          <section className="panel" aria-labelledby="match-list-title">
            <h2 id="match-list-title">입력 대상 경기</h2>
            <div className="match-button-list" role="list">
              {dataset.matches.map((match) => (
                <div key={match.id} role="listitem">
                  <button
                    type="button"
                    className="match-select-button"
                    aria-pressed={selectedMatch.id === match.id}
                    onClick={() => selectMatch(match)}
                  >
                    경기 {match.matchNo}번. {formatMatchTime(match.scheduledAt)}.{" "}
                    {getDivisionName(match.divisionId, dataset.divisions)}. {matchStatusLabel(match.status)}.
                    <br />
                    {getPlayerName(match.player1TournamentPlayerId, dataset.tournamentPlayers, dataset.players)} 대{" "}
                    {getPlayerName(match.player2TournamentPlayerId, dataset.tournamentPlayers, dataset.players)}
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="panel" aria-labelledby="score-form-title">
            <h2 id="score-form-title">선택 경기 점수</h2>
            <dl className="meta-list">
              <div>
                <dt>경기</dt>
                <dd>{selectedMatch.matchNo}번</dd>
              </div>
              <div>
                <dt>그룹</dt>
                <dd>{getGroupName(selectedMatch.groupId, dataset.groups)}</dd>
              </div>
              <div>
                <dt>선수</dt>
                <dd>
                  {player1Name} 대 {player2Name}
                </dd>
              </div>
              <div>
                <dt>버전</dt>
                <dd>{selectedMatch.version}</dd>
              </div>
            </dl>

            <div aria-live="polite" aria-atomic="true" className="alert" role="status">
              {liveMessage}
            </div>

            {errors.length > 0 ? (
              <div className="alert error" role="alert" id="score-errors">
                <strong>입력 오류</strong>
                <ul>
                  {errors.map((error) => (
                    <li key={error}>{error}</li>
                  ))}
                </ul>
              </div>
            ) : null}

            <form ref={formRef} tabIndex={-1} onSubmit={onSubmit} aria-describedby={errors.length ? "score-errors" : undefined}>
              <fieldset>
                <legend>세트별 점수</legend>
                <div className="set-grid">
                  {sets.map((set, index) => (
                    <div className="set-row" key={set.setNo}>
                      <strong>{set.setNo}세트</strong>
                      <label className="field">
                        <span>{player1Name} 점수</span>
                        <input
                          inputMode="numeric"
                          min="0"
                          type="number"
                          value={Number.isNaN(set.player1Score) ? "" : set.player1Score}
                          onChange={(event) => updateSetScore(index, "player1Score", event.target.value)}
                        />
                      </label>
                      <label className="field">
                        <span>{player2Name} 점수</span>
                        <input
                          inputMode="numeric"
                          min="0"
                          type="number"
                          value={Number.isNaN(set.player2Score) ? "" : set.player2Score}
                          onChange={(event) => updateSetScore(index, "player2Score", event.target.value)}
                        />
                      </label>
                    </div>
                  ))}
                </div>
              </fieldset>

              <section aria-labelledby="preview-title">
                <h3 id="preview-title">입력 결과 미리보기</h3>
                {preview ? (
                  <dl className="meta-list">
                    <div>
                      <dt>예상 승자</dt>
                      <dd>{preview.winnerName}</dd>
                    </div>
                    <div>
                      <dt>세트 승수</dt>
                      <dd>
                        {player1Name} {preview.player1SetsWon}세트, {player2Name} {preview.player2SetsWon}세트
                      </dd>
                    </div>
                    <div>
                      <dt>총 득점</dt>
                      <dd>
                        {player1Name} {preview.player1TotalPoints}점, {player2Name} {preview.player2TotalPoints}점
                      </dd>
                    </div>
                  </dl>
                ) : (
                  <p>점수를 입력하면 결과 미리보기가 표시됩니다.</p>
                )}
              </section>

              <div className="toolbar">
                <button type="submit" disabled={isSaving}>
                  {isSaving ? "저장 중" : "세트 점수 저장"}
                </button>
                <button
                  className="secondary"
                  type="button"
                  onClick={() => {
                    setSets(deriveInitialSets(selectedMatch));
                    setErrors([]);
                    setLiveMessage("입력값을 선택 경기의 기존 점수로 되돌렸습니다.");
                  }}
                >
                  입력값 되돌리기
                </button>
              </div>
            </form>
          </section>
        </div>
    </AdminFrame>
  );
}

function deriveInitialSets(match: Match): ScoreSetInput[] {
  if (match.sets.length > 0) {
    const existing = match.sets.map((set) => ({
      setNo: set.setNo,
      player1Score: set.player1Score,
      player2Score: set.player2Score,
    }));
    return [...existing, ...initialSets.slice(existing.length)].slice(0, 3);
  }
  return initialSets;
}

function validateSets(sets: ScoreSetInput[]) {
  const errors: string[] = [];

  sets.forEach((set) => {
    if (Number.isNaN(set.player1Score) || Number.isNaN(set.player2Score)) {
      errors.push(`${set.setNo}세트 점수는 두 선수 모두 입력해야 합니다.`);
    }
    if (set.player1Score < 0 || set.player2Score < 0) {
      errors.push(`${set.setNo}세트 점수는 0 이상이어야 합니다.`);
    }
    if (set.player1Score === set.player2Score) {
      errors.push(`${set.setNo}세트는 동점으로 저장할 수 없습니다.`);
    }
  });

  const player1Wins = sets.filter((set) => set.player1Score > set.player2Score).length;
  const player2Wins = sets.filter((set) => set.player2Score > set.player1Score).length;
  if (player1Wins === player2Wins) {
    errors.push("전체 세트 승수가 동률입니다. 승자가 결정되도록 점수를 입력하세요.");
  }

  return errors;
}
