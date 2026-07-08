"use client";

import { useEffect, useState } from "react";
import { AdminFrame } from "@/components/AdminFrame";
import { EmptyState, ErrorState } from "@/components/StateViews";
import {
  createDivision,
  createGroup,
  createMatch,
  createOfficial,
  createStage,
  createTournament,
  createTournamentPlayer,
  addGroupMember,
  deleteDivision,
  deleteGroup,
  deleteMatch,
  deleteOfficial,
  deleteStage,
  deleteTournamentPlayer,
  getAdminDataset,
  getGroupMembers,
  generateRoundRobin,
  previewRoundRobin,
  removeGroupMember,
  updateGroup,
  updateMatch,
  updateOfficial,
  updateStage,
  updateTournament,
  updateTournamentPlayer,
  type BasicAuthSession,
  type DivisionRequest,
  type GroupRequest,
  type GroupMemberResponse,
  type RoundRobinPreview,
  type MatchRequest,
  type OfficialRequest,
  type StageRequest,
  type TournamentPlayerRequest,
  type TournamentRequest,
} from "@/lib/api";
import { clearAuthSession, readAuthSession } from "@/lib/auth";
import {
  formatMatchTime,
  getDivisionName,
  getGroupName,
  getPlayerName,
  matchStatusLabel,
} from "@/lib/helpers";
import type { Group, Match, Official, Stage, Tournament, TournamentDataset, TournamentPlayer } from "@/lib/types";

export type AdminSection = "dashboard" | "tournament" | "players" | "officials" | "stages" | "groups" | "matches";

export function AdminCrudWorkspace({ section }: { section: AdminSection }) {
  const [auth] = useState<BasicAuthSession | null>(() => readAuthSession());
  const [dataset, setDataset] = useState<TournamentDataset | null>(null);
  const [loading, setLoading] = useState(() => Boolean(readAuthSession()));
  const [message, setMessage] = useState(() =>
    readAuthSession() ? "관리자 데이터를 불러오는 중입니다." : "로그인이 필요합니다.",
  );
  const [error, setError] = useState<string | null>(null);

  async function reload(nextAuth = auth) {
    if (!nextAuth) return;
    setLoading(true);
    setError(null);
    try {
      setDataset(await getAdminDataset(nextAuth, dataset?.tournament.id || undefined));
      setMessage("서버 데이터와 동기화되었습니다.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "관리자 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!auth) return;
    const timer = window.setTimeout(() => {
      void reload(auth);
    }, 0);
    return () => window.clearTimeout(timer);
    // 최초 진입 때 sessionStorage 인증을 읽어 API 호출에 붙인다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!auth) {
    return (
      <AdminFrame tournamentCode="seoul-open-2026" currentPath={section === "dashboard" ? "/admin" : `/admin/${section}`}>
        <EmptyState
          title="관리자 로그인이 필요합니다"
          description="관리자 CRUD와 점수 입력 API는 Basic Auth 인증 후 사용할 수 있습니다."
          actionHref="/admin/login"
          actionLabel="로그인으로 이동"
        />
      </AdminFrame>
    );
  }

  const currentPath = section === "dashboard" ? "/admin" : `/admin/${section}`;
  const tournamentCode = dataset?.tournament.code || "seoul-open-2026";

  return (
    <AdminFrame tournamentCode={tournamentCode} currentPath={currentPath}>
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">관리자 화면</p>
        <h1 id="page-title">{adminTitle(section)}</h1>
        <p className="lead">
          백엔드 REST API와 직접 연결된 화면입니다. 저장 후에는 서버 데이터를 다시 조회해 화면을 동기화합니다.
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

      <div aria-live="polite" aria-atomic="true" className="alert" role="status">
        {message}
      </div>

      {loading ? <EmptyState title="불러오는 중" description="백엔드 API에서 최신 데이터를 가져오고 있습니다." /> : null}
      {error ? <ErrorState title="관리자 API 오류" message={error} /> : null}
      {!loading && !error && !dataset ? (
        <EmptyState title="데이터 없음" description="관리자 API 응답을 표시할 수 없습니다." />
      ) : null}
      {!loading && !error && dataset ? (
        <AdminSectionView
          auth={auth}
          dataset={dataset}
          section={section}
          onMessage={setMessage}
          onReload={() => reload(auth)}
        />
      ) : null}
    </AdminFrame>
  );
}

function AdminSectionView({
  auth,
  dataset,
  section,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  section: AdminSection;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  if (!dataset.tournament.id) {
    return <TournamentCreatePanel auth={auth} onMessage={onMessage} onReload={onReload} />;
  }

  if (section === "dashboard") return <Dashboard dataset={dataset} />;
  if (section === "tournament") {
    return <TournamentPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
  }
  if (section === "players") {
    return <PlayersPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
  }
  if (section === "officials") {
    return <OfficialsPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
  }
  if (section === "stages") {
    return <StagesPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
  }
  if (section === "groups") {
    return <GroupsPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
  }
  return <MatchesPanel auth={auth} dataset={dataset} onMessage={onMessage} onReload={onReload} />;
}

function Dashboard({ dataset }: { dataset: TournamentDataset }) {
  const completed = dataset.matches.filter((match) => match.status === "completed").length;
  const running = dataset.matches.filter((match) => match.status === "running").length;
  const scheduled = dataset.matches.filter((match) => match.status === "scheduled").length;

  return (
    <section aria-labelledby="dashboard-title">
      <h2 id="dashboard-title">운영 현황</h2>
      <div className="grid">
        <Metric title="부문" value={`${dataset.divisions.length}개`} />
        <Metric title="참가 선수" value={`${dataset.tournamentPlayers.length}명`} />
        <Metric title="심판" value={`${dataset.officials.length}명`} />
        <Metric title="단계" value={`${dataset.stages.length}개`} />
        <Metric title="조" value={`${dataset.groups.length}개`} />
        <Metric title="미입력 경기" value={`${scheduled}경기`} />
        <Metric title="진행 중 경기" value={`${running}경기`} />
        <Metric title="완료 경기" value={`${completed}경기`} />
      </div>
    </section>
  );
}

function Metric({ title, value }: { title: string; value: string }) {
  return (
    <div className="panel">
      <h3>{title}</h3>
      <p>{value}</p>
    </div>
  );
}

function TournamentCreatePanel({
  auth,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [draft, setDraft] = useState<TournamentRequest>({
    code: "seoul-open-2026",
    name: "서울 쇼다운 오픈 2026",
    location: "서울",
    startDate: new Date().toISOString().slice(0, 10),
    endDate: new Date().toISOString().slice(0, 10),
    timezone: "Asia/Seoul",
    status: "DRAFT",
    defaultLanguage: "ko",
  });

  return (
    <section className="panel" aria-labelledby="create-tournament-title">
      <h2 id="create-tournament-title">첫 대회 생성</h2>
      <TournamentForm
        draft={draft}
        submitLabel="대회 생성"
        onChange={setDraft}
        onSubmit={async () => {
          await createTournament(auth, draft);
          onMessage("대회를 생성했습니다.");
          await onReload();
        }}
      />
    </section>
  );
}

function TournamentPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [draft, setDraft] = useState<TournamentRequest>(toTournamentRequest(dataset.tournament));
  const [divisionDraft, setDivisionDraft] = useState<DivisionRequest>({
    name: "",
    code: "",
    category: "OPEN",
    sortOrder: dataset.divisions.length + 1,
    active: true,
  });

  return (
    <div className="admin-crud-layout">
      <section className="panel" aria-labelledby="tournament-form-title">
        <h2 id="tournament-form-title">대회 정보</h2>
        <TournamentForm
          draft={draft}
          submitLabel="대회 수정 저장"
          onChange={setDraft}
          onSubmit={async () => {
            await updateTournament(auth, dataset.tournament.id, draft);
            onMessage("대회 정보를 저장했습니다.");
            await onReload();
          }}
        />
      </section>

      <section aria-labelledby="division-title">
        <h2 id="division-title">부문 CRUD</h2>
        <form
          className="panel form-grid"
          onSubmit={async (event) => {
            event.preventDefault();
            await createDivision(auth, dataset.tournament.id, divisionDraft);
            setDivisionDraft({ name: "", code: "", category: "OPEN", sortOrder: dataset.divisions.length + 2, active: true });
            onMessage("부문을 생성했습니다.");
            await onReload();
          }}
        >
          <label className="field">
            <span>부문 이름</span>
            <input value={divisionDraft.name} onChange={(event) => setDivisionDraft({ ...divisionDraft, name: event.target.value })} required />
          </label>
          <label className="field">
            <span>코드</span>
            <input value={divisionDraft.code} onChange={(event) => setDivisionDraft({ ...divisionDraft, code: event.target.value })} required />
          </label>
          <button type="submit">부문 추가</button>
        </form>
        <SimpleTable
          caption="부문 목록"
          headers={["이름", "코드", "작업"]}
          rows={dataset.divisions.map((division) => [
            division.name,
            division.code,
            <button
              key={division.id}
              className="secondary"
              type="button"
              onClick={async () => {
                await deleteDivision(auth, division.id);
                onMessage(`${division.name} 부문을 삭제했습니다.`);
                await onReload();
              }}
            >
              삭제
            </button>,
          ])}
        />
      </section>
    </div>
  );
}

function PlayersPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<TournamentPlayerRequest>(emptyPlayerRequest(dataset.divisions[0]?.id ?? ""));

  return (
    <CrudShell
      title={editingId ? "선수 수정" : "선수 추가"}
      form={
        <PlayerForm
          dataset={dataset}
          draft={draft}
          submitLabel={editingId ? "선수 수정 저장" : "선수 추가"}
          onChange={setDraft}
          onSubmit={async () => {
            if (editingId) await updateTournamentPlayer(auth, editingId, draft);
            else await createTournamentPlayer(auth, dataset.tournament.id, draft);
            setEditingId(null);
            setDraft(emptyPlayerRequest(dataset.divisions[0]?.id ?? ""));
            onMessage("선수 정보를 저장했습니다.");
            await onReload();
          }}
        />
      }
      list={
        <SimpleTable
          caption="참가 선수 목록"
          headers={["이름", "부문", "참가 번호", "작업"]}
          rows={dataset.tournamentPlayers.map((entry) => [
            getPlayerName(entry.id, dataset.tournamentPlayers, dataset.players),
            getDivisionName(entry.divisionId, dataset.divisions),
            String(entry.entryNo || ""),
            <div className="row-actions" key={entry.id}>
              <button
                className="secondary"
                type="button"
                onClick={() => {
                  setEditingId(entry.id);
                  setDraft(toPlayerRequest(entry, dataset));
                }}
              >
                수정
              </button>
              <button
                className="secondary"
                type="button"
                onClick={async () => {
                  await deleteTournamentPlayer(auth, entry.id);
                  onMessage("선수를 삭제했습니다.");
                  await onReload();
                }}
              >
                삭제
              </button>
            </div>,
          ])}
        />
      }
    />
  );
}

function GroupsPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<GroupRequest>(emptyGroupRequest(dataset));
  const [selectedGroupId, setSelectedGroupId] = useState(dataset.groups[0]?.id ?? "");
  const [selectedPlayerId, setSelectedPlayerId] = useState(dataset.tournamentPlayers[0]?.id ?? "");
  const [slotNo, setSlotNo] = useState(1);
  const [members, setMembers] = useState<GroupMemberResponse[]>([]);
  const [preview, setPreview] = useState<RoundRobinPreview | null>(null);

  async function loadMembers(groupId = selectedGroupId) {
    if (!groupId) return;
    setMembers(await getGroupMembers(auth, groupId));
    setPreview(null);
  }

  if (dataset.stages.length === 0 || dataset.divisions.length === 0) {
    return <EmptyState title="조 생성 전 단계가 필요합니다" description="먼저 백엔드 Swagger 또는 단계 API로 단계와 부문을 생성하세요." />;
  }

  return (
    <CrudShell
      title={editingId ? "조 수정" : "조 추가"}
      form={
        <GroupForm
          dataset={dataset}
          draft={draft}
          submitLabel={editingId ? "조 수정 저장" : "조 추가"}
          onChange={setDraft}
          onSubmit={async () => {
            if (editingId) await updateGroup(auth, editingId, draft);
            else await createGroup(auth, dataset.tournament.id, draft);
            setEditingId(null);
            setDraft(emptyGroupRequest(dataset));
            onMessage("조 정보를 저장했습니다.");
            await onReload();
          }}
        />
      }
      list={
        <>
        <SimpleTable
          caption="조 목록"
          headers={["이름", "코드", "부문", "작업"]}
          rows={dataset.groups.map((group) => [
            group.name,
            group.code,
            getDivisionName(group.divisionId, dataset.divisions),
            <div className="row-actions" key={group.id}>
              <button
                className="secondary"
                type="button"
                onClick={() => {
                  setEditingId(group.id);
                  setDraft(toGroupRequest(group));
                }}
              >
                수정
              </button>
              <button
                className="secondary"
                type="button"
                onClick={async () => {
                  await deleteGroup(auth, group.id);
                  onMessage("조를 삭제했습니다.");
                  await onReload();
                }}
              >
                삭제
              </button>
            </div>,
          ])}
        />
        <section className="panel" aria-labelledby="group-member-title">
          <h3 id="group-member-title">조 구성원 배정</h3>
          <div className="form-grid">
            <label className="field"><span>조</span><select value={selectedGroupId} onChange={(event) => { setSelectedGroupId(event.target.value); void loadMembers(event.target.value); }}>
              {dataset.groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
            </select></label>
            <label className="field"><span>선수</span><select value={selectedPlayerId} onChange={(event) => setSelectedPlayerId(event.target.value)}>
              {dataset.tournamentPlayers.map((player) => <option key={player.id} value={player.id}>{getPlayerName(player.id, dataset.tournamentPlayers, dataset.players)}</option>)}
            </select></label>
            <label className="field"><span>슬롯</span><input type="number" min="1" value={slotNo} onChange={(event) => setSlotNo(Number(event.target.value))} /></label>
          </div>
          <div className="toolbar">
            <button type="button" onClick={async () => { await addGroupMember(auth, selectedGroupId, selectedPlayerId, slotNo); await loadMembers(); onMessage("선수를 조에 배정했습니다."); }}>선수 배정</button>
            <button className="secondary" type="button" onClick={() => loadMembers()}>구성원 조회</button>
            <button className="secondary" type="button" onClick={async () => setPreview(await previewRoundRobin(auth, selectedGroupId))}>대진 미리보기</button>
            <button type="button" disabled={!preview || dataset.officials.length < 2} onClick={async () => {
              const result = await generateRoundRobin(auth, selectedGroupId, {
                startAt: `${dataset.tournament.startDate}T09:00:00+09:00`, matchDurationMinutes: 30, courtNames: ["1"],
                officialIds: dataset.officials.map((official) => official.id),
              });
              onMessage(`라운드로빈 ${result.createdMatchCount}경기를 생성했습니다.`);
              await onReload();
            }}>미리보기대로 생성</button>
          </div>
          {members.length ? <ul>{members.map((member) => <li key={member.id}>{member.slotNo}번 {member.playerName} <button className="secondary" type="button" onClick={async () => { await removeGroupMember(auth, selectedGroupId, member.id); await loadMembers(); }}>배정 해제</button></li>)}</ul> : <p>구성원 조회 버튼을 눌러 현재 배정을 확인하세요.</p>}
          {preview ? <p aria-live="polite">예상 경기 수: {preview.expectedMatchCount}경기 ({preview.matches.map((match) => `${match.player1Name} 대 ${match.player2Name}`).join(", ")})</p> : null}
        </section>
        </>
      }
    />
  );
}

function OfficialsPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<OfficialRequest>(emptyOfficialRequest());

  return (
    <CrudShell
      title={editingId ? "심판 수정" : "심판 추가"}
      form={
        <OfficialForm
          draft={draft}
          submitLabel={editingId ? "심판 수정 저장" : "심판 추가"}
          onChange={setDraft}
          onSubmit={async () => {
            if (editingId) await updateOfficial(auth, editingId, draft);
            else await createOfficial(auth, dataset.tournament.id, draft);
            setEditingId(null);
            setDraft(emptyOfficialRequest());
            onMessage("심판 정보를 저장했습니다.");
            await onReload();
          }}
        />
      }
      list={
        <SimpleTable
          caption="심판 목록"
          headers={["이름", "코드", "역할", "상태", "작업"]}
          rows={dataset.officials.map((official) => [
            official.name,
            official.shortCode ?? "",
            official.roleName,
            official.active ? "활성" : "비활성",
            <div className="row-actions" key={official.id}>
              <button
                className="secondary"
                type="button"
                onClick={() => {
                  setEditingId(official.id);
                  setDraft(toOfficialRequest(official));
                }}
              >
                수정
              </button>
              <button
                className="secondary"
                type="button"
                onClick={async () => {
                  await deleteOfficial(auth, official.id);
                  onMessage("심판을 삭제했습니다.");
                  await onReload();
                }}
              >
                삭제
              </button>
            </div>,
          ])}
        />
      }
    />
  );
}

function StagesPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<StageRequest>(emptyStageRequest(dataset));

  if (dataset.divisions.length === 0) {
    return <EmptyState title="단계 생성 전 부문이 필요합니다" description="먼저 대회 설정에서 부문을 생성하세요." />;
  }

  return (
    <CrudShell
      title={editingId ? "단계 수정" : "단계 추가"}
      form={
        <StageForm
          dataset={dataset}
          draft={draft}
          submitLabel={editingId ? "단계 수정 저장" : "단계 추가"}
          onChange={setDraft}
          onSubmit={async () => {
            if (editingId) await updateStage(auth, editingId, draft);
            else await createStage(auth, dataset.tournament.id, draft);
            setEditingId(null);
            setDraft(emptyStageRequest(dataset));
            onMessage("단계 정보를 저장했습니다.");
            await onReload();
          }}
        />
      }
      list={
        <SimpleTable
          caption="단계 목록"
          headers={["이름", "부문", "유형", "순서", "작업"]}
          rows={dataset.stages.map((stage) => [
            stage.name,
            getDivisionName(stage.divisionId, dataset.divisions),
            stage.stageType === "round_robin" ? "리그" : stage.stageType === "knockout" ? "토너먼트" : "순위전",
            String(stage.sortOrder),
            <div className="row-actions" key={stage.id}>
              <button
                className="secondary"
                type="button"
                onClick={() => {
                  setEditingId(stage.id);
                  setDraft(toStageRequest(stage));
                }}
              >
                수정
              </button>
              <button
                className="secondary"
                type="button"
                onClick={async () => {
                  await deleteStage(auth, stage.id);
                  onMessage("단계를 삭제했습니다.");
                  await onReload();
                }}
              >
                삭제
              </button>
            </div>,
          ])}
        />
      }
    />
  );
}

function MatchesPanel({
  auth,
  dataset,
  onMessage,
  onReload,
}: {
  auth: BasicAuthSession;
  dataset: TournamentDataset;
  onMessage: (message: string) => void;
  onReload: () => Promise<void>;
}) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<MatchRequest>(emptyMatchRequest(dataset));

  if (dataset.stages.length === 0 || dataset.tournamentPlayers.length < 2) {
    return <EmptyState title="경기 생성 준비가 필요합니다" description="경기를 만들려면 단계와 참가 선수 2명 이상이 필요합니다." />;
  }

  return (
    <CrudShell
      title={editingId ? "경기 수정" : "경기 추가"}
      form={
        <MatchForm
          dataset={dataset}
          draft={draft}
          submitLabel={editingId ? "경기 수정 저장" : "경기 추가"}
          onChange={setDraft}
          onSubmit={async () => {
            if (editingId) await updateMatch(auth, editingId, draft);
            else await createMatch(auth, dataset.tournament.id, draft);
            setEditingId(null);
            setDraft(emptyMatchRequest(dataset));
            onMessage("경기 정보를 저장했습니다.");
            await onReload();
          }}
        />
      }
      list={
        <SimpleTable
          caption="경기 목록"
          headers={["경기", "시간", "그룹", "선수", "상태", "작업"]}
          rows={dataset.matches.map((match) => [
            `${match.matchNo}번`,
            formatMatchTime(match.scheduledAt),
            getGroupName(match.groupId, dataset.groups),
            `${getPlayerName(match.player1TournamentPlayerId, dataset.tournamentPlayers, dataset.players)} 대 ${getPlayerName(
              match.player2TournamentPlayerId,
              dataset.tournamentPlayers,
              dataset.players,
            )}`,
            matchStatusLabel(match.status),
            <div className="row-actions" key={match.id}>
              <button
                className="secondary"
                type="button"
                onClick={() => {
                  setEditingId(match.id);
                  setDraft(toMatchRequest(match));
                }}
              >
                수정
              </button>
              <button
                className="secondary"
                type="button"
                onClick={async () => {
                  await deleteMatch(auth, match.id);
                  onMessage("경기를 삭제했습니다.");
                  await onReload();
                }}
              >
                삭제
              </button>
            </div>,
          ])}
        />
      }
    />
  );
}

function CrudShell({ title, form, list }: { title: string; form: React.ReactNode; list: React.ReactNode }) {
  return (
    <div className="admin-crud-layout">
      <section className="panel" aria-labelledby="crud-form-title">
        <h2 id="crud-form-title">{title}</h2>
        {form}
      </section>
      <section aria-labelledby="crud-list-title">
        <h2 id="crud-list-title">목록</h2>
        {list}
      </section>
    </div>
  );
}

function TournamentForm({
  draft,
  submitLabel,
  onChange,
  onSubmit,
}: {
  draft: TournamentRequest;
  submitLabel: string;
  onChange: (draft: TournamentRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form
      className="form-grid"
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit();
      }}
    >
      <label className="field">
        <span>대회 이름</span>
        <input value={draft.name} onChange={(event) => onChange({ ...draft, name: event.target.value })} required />
      </label>
      <label className="field">
        <span>대회 코드</span>
        <input value={draft.code} onChange={(event) => onChange({ ...draft, code: event.target.value })} required />
      </label>
      <label className="field">
        <span>장소</span>
        <input value={draft.location ?? ""} onChange={(event) => onChange({ ...draft, location: event.target.value })} />
      </label>
      <label className="field">
        <span>시작일</span>
        <input type="date" value={draft.startDate} onChange={(event) => onChange({ ...draft, startDate: event.target.value })} required />
      </label>
      <label className="field">
        <span>종료일</span>
        <input type="date" value={draft.endDate} onChange={(event) => onChange({ ...draft, endDate: event.target.value })} required />
      </label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function PlayerForm({ dataset, draft, submitLabel, onChange, onSubmit }: {
  dataset: TournamentDataset;
  draft: TournamentPlayerRequest;
  submitLabel: string;
  onChange: (draft: TournamentPlayerRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); void onSubmit(); }}>
      <label className="field"><span>선수 이름</span><input value={draft.displayName} onChange={(event) => onChange({ ...draft, displayName: event.target.value })} required /></label>
      <label className="field"><span>국가 코드</span><input value={draft.countryCode ?? ""} onChange={(event) => onChange({ ...draft, countryCode: event.target.value.toUpperCase() })} maxLength={3} /></label>
      <label className="field"><span>부문</span><select value={draft.divisionId} onChange={(event) => onChange({ ...draft, divisionId: event.target.value })}>{dataset.divisions.map((division) => <option key={division.id} value={division.id}>{division.name}</option>)}</select></label>
      <label className="field"><span>참가 번호</span><input type="number" min="1" value={draft.entryNo ?? ""} onChange={(event) => onChange({ ...draft, entryNo: Number(event.target.value) })} /></label>
      <label className="field"><span>시드 번호</span><input type="number" min="1" value={draft.seedNo ?? ""} onChange={(event) => onChange({ ...draft, seedNo: event.target.value ? Number(event.target.value) : undefined })} /></label>
      <label className="field"><span>소속</span><input value={draft.clubName ?? ""} onChange={(event) => onChange({ ...draft, clubName: event.target.value })} /></label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function GroupForm({ dataset, draft, submitLabel, onChange, onSubmit }: {
  dataset: TournamentDataset;
  draft: GroupRequest;
  submitLabel: string;
  onChange: (draft: GroupRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); void onSubmit(); }}>
      <label className="field"><span>조 이름</span><input value={draft.name} onChange={(event) => onChange({ ...draft, name: event.target.value })} required /></label>
      <label className="field"><span>조 코드</span><input value={draft.code} onChange={(event) => onChange({ ...draft, code: event.target.value })} required /></label>
      <label className="field"><span>부문</span><select value={draft.divisionId} onChange={(event) => onChange({ ...draft, divisionId: event.target.value })}>{dataset.divisions.map((division) => <option key={division.id} value={division.id}>{division.name}</option>)}</select></label>
      <label className="field"><span>단계</span><select value={draft.stageId} onChange={(event) => onChange({ ...draft, stageId: event.target.value })}>{dataset.stages.map((stage) => <option key={stage.id} value={stage.id}>{stage.name}</option>)}</select></label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function OfficialForm({ draft, submitLabel, onChange, onSubmit }: {
  draft: OfficialRequest;
  submitLabel: string;
  onChange: (draft: OfficialRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); void onSubmit(); }}>
      <label className="field"><span>심판 이름</span><input value={draft.name} onChange={(event) => onChange({ ...draft, name: event.target.value })} required /></label>
      <label className="field"><span>표시 코드</span><input value={draft.shortCode ?? ""} onChange={(event) => onChange({ ...draft, shortCode: event.target.value })} /></label>
      <label className="field"><span>역할명</span><input value={draft.roleName} onChange={(event) => onChange({ ...draft, roleName: event.target.value })} required /></label>
      <label className="field"><span>활성 상태</span><select value={draft.active === false ? "false" : "true"} onChange={(event) => onChange({ ...draft, active: event.target.value === "true" })}><option value="true">활성</option><option value="false">비활성</option></select></label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function StageForm({ dataset, draft, submitLabel, onChange, onSubmit }: {
  dataset: TournamentDataset;
  draft: StageRequest;
  submitLabel: string;
  onChange: (draft: StageRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); void onSubmit(); }}>
      <label className="field"><span>단계 이름</span><input value={draft.name} onChange={(event) => onChange({ ...draft, name: event.target.value })} required /></label>
      <label className="field"><span>부문</span><select value={draft.divisionId} onChange={(event) => onChange({ ...draft, divisionId: event.target.value })}>{dataset.divisions.map((division) => <option key={division.id} value={division.id}>{division.name}</option>)}</select></label>
      <label className="field"><span>단계 유형</span><select value={draft.stageType} onChange={(event) => onChange({ ...draft, stageType: event.target.value as StageRequest["stageType"] })}><option value="ROUND_ROBIN">리그</option><option value="KNOCKOUT">토너먼트</option><option value="PLACEMENT">순위전</option></select></label>
      <label className="field"><span>정렬 순서</span><input type="number" min="0" value={draft.sortOrder ?? 0} onChange={(event) => onChange({ ...draft, sortOrder: Number(event.target.value) })} /></label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function MatchForm({ dataset, draft, submitLabel, onChange, onSubmit }: {
  dataset: TournamentDataset;
  draft: MatchRequest;
  submitLabel: string;
  onChange: (draft: MatchRequest) => void;
  onSubmit: () => Promise<void>;
}) {
  return (
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); void onSubmit(); }}>
      <label className="field"><span>경기 번호</span><input type="number" min="1" value={draft.matchNo} onChange={(event) => onChange({ ...draft, matchNo: Number(event.target.value) })} required /></label>
      <label className="field"><span>부문</span><select value={draft.divisionId} onChange={(event) => onChange({ ...draft, divisionId: event.target.value })}>{dataset.divisions.map((division) => <option key={division.id} value={division.id}>{division.name}</option>)}</select></label>
      <label className="field"><span>단계</span><select value={draft.stageId} onChange={(event) => onChange({ ...draft, stageId: event.target.value })}>{dataset.stages.map((stage) => <option key={stage.id} value={stage.id}>{stage.name}</option>)}</select></label>
      <label className="field"><span>그룹</span><select value={draft.groupId ?? ""} onChange={(event) => onChange({ ...draft, groupId: event.target.value || undefined })}><option value="">미지정</option>{dataset.groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select></label>
      <label className="field"><span>시작 시각</span><input type="datetime-local" value={(draft.scheduledAt ?? "").slice(0, 16)} onChange={(event) => onChange({ ...draft, scheduledAt: `${event.target.value}:00+09:00` })} /></label>
      <label className="field"><span>테이블</span><input value={draft.courtName ?? ""} onChange={(event) => onChange({ ...draft, courtName: event.target.value })} /></label>
      <label className="field"><span>경기 시간(분)</span><input type="number" min="1" value={draft.durationMinutes ?? 30} onChange={(event) => onChange({ ...draft, durationMinutes: Number(event.target.value) })} /></label>
      <label className="field"><span>경기 형식</span><select value={draft.maxSets ?? 3} onChange={(event) => onChange({ ...draft, maxSets: Number(event.target.value) as 1 | 3 | 5 })}><option value="1">1세트제</option><option value="3">3세트제</option><option value="5">5세트제</option></select></label>
      <label className="field"><span>심판 1</span><select value={draft.refereeOfficialIds?.[0] ?? ""} onChange={(event) => onChange({ ...draft, refereeOfficialIds: [event.target.value, draft.refereeOfficialIds?.[1] ?? ""] })}>{dataset.officials.map((official) => <option key={official.id} value={official.id}>{official.name}</option>)}</select></label>
      <label className="field"><span>심판 2</span><select value={draft.refereeOfficialIds?.[1] ?? ""} onChange={(event) => onChange({ ...draft, refereeOfficialIds: [draft.refereeOfficialIds?.[0] ?? "", event.target.value] })}>{dataset.officials.map((official) => <option key={official.id} value={official.id}>{official.name}</option>)}</select></label>
      <label className="field"><span>선수 1</span><select value={draft.player1TournamentPlayerId} onChange={(event) => onChange({ ...draft, player1TournamentPlayerId: event.target.value })}>{dataset.tournamentPlayers.map((entry) => <option key={entry.id} value={entry.id}>{getPlayerName(entry.id, dataset.tournamentPlayers, dataset.players)}</option>)}</select></label>
      <label className="field"><span>선수 2</span><select value={draft.player2TournamentPlayerId} onChange={(event) => onChange({ ...draft, player2TournamentPlayerId: event.target.value })}>{dataset.tournamentPlayers.map((entry) => <option key={entry.id} value={entry.id}>{getPlayerName(entry.id, dataset.tournamentPlayers, dataset.players)}</option>)}</select></label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function SimpleTable({ caption, headers, rows }: { caption: string; headers: string[]; rows: React.ReactNode[][] }) {
  if (rows.length === 0) return <EmptyState title={`${caption} 없음`} description="아직 등록된 데이터가 없습니다." />;
  return (
    <div className="table-wrap">
      <table>
        <caption>{caption}</caption>
        <thead><tr>{headers.map((header) => <th key={header} scope="col">{header}</th>)}</tr></thead>
        <tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}

function adminTitle(section: AdminSection) {
  const titles: Record<AdminSection, string> = {
    dashboard: "관리자 대시보드",
    tournament: "대회 설정",
    players: "선수 관리",
    officials: "심판 관리",
    stages: "단계 관리",
    groups: "조 편성",
    matches: "일정 관리",
  };
  return titles[section];
}

function toTournamentRequest(tournament: Tournament): TournamentRequest {
  return {
    code: tournament.code,
    name: tournament.name,
    location: tournament.location,
    startDate: tournament.startDate,
    endDate: tournament.endDate,
    timezone: tournament.timezone,
    status: tournament.status.toUpperCase() as TournamentRequest["status"],
    defaultLanguage: "ko",
  };
}

function emptyPlayerRequest(divisionId: string): TournamentPlayerRequest {
  return { displayName: "", countryCode: "KOR", divisionId, status: "ACTIVE" };
}

function toPlayerRequest(entry: TournamentPlayer, dataset: TournamentDataset): TournamentPlayerRequest {
  const player = dataset.players.find((item) => item.id === entry.playerId);
  return {
    displayName: entry.displayNameOverride ?? player?.displayName ?? "",
    countryCode: player?.countryCode,
    divisionId: entry.divisionId,
    seedNo: entry.seedNo,
    entryNo: entry.entryNo || undefined,
    displayNameOverride: entry.displayNameOverride,
    clubName: entry.clubName,
    status: entry.status.toUpperCase() as TournamentPlayerRequest["status"],
  };
}

function emptyGroupRequest(dataset: TournamentDataset): GroupRequest {
  return {
    divisionId: dataset.divisions[0]?.id ?? "",
    stageId: dataset.stages[0]?.id ?? "",
    code: "",
    name: "",
    groupType: "LEAGUE",
  };
}

function toGroupRequest(group: Group): GroupRequest {
  return {
    divisionId: group.divisionId,
    stageId: group.stageId,
    code: group.code,
    name: group.name,
    groupType: group.groupType.toUpperCase() as GroupRequest["groupType"],
    sortOrder: group.sortOrder,
  };
}

function emptyOfficialRequest(): OfficialRequest {
  return { name: "", shortCode: "", roleName: "REFEREE", active: true };
}

function toOfficialRequest(official: Official): OfficialRequest {
  return {
    name: official.name,
    shortCode: official.shortCode,
    roleName: official.roleName,
    active: official.active,
  };
}

function emptyStageRequest(dataset: TournamentDataset): StageRequest {
  return {
    divisionId: dataset.divisions[0]?.id ?? "",
    name: "",
    stageType: "ROUND_ROBIN",
    sortOrder: dataset.stages.length + 1,
  };
}

function toStageRequest(stage: Stage): StageRequest {
  return {
    divisionId: stage.divisionId,
    name: stage.name,
    stageType: stage.stageType.toUpperCase() as StageRequest["stageType"],
    sortOrder: stage.sortOrder,
  };
}

function emptyMatchRequest(dataset: TournamentDataset): MatchRequest {
  return {
    divisionId: dataset.divisions[0]?.id ?? "",
    stageId: dataset.stages[0]?.id ?? "",
    groupId: dataset.groups[0]?.id,
    matchNo: dataset.matches.length + 1,
    scheduledAt: new Date().toISOString(),
    courtName: "",
    durationMinutes: 30,
    maxSets: 3,
    refereeOfficialIds: dataset.officials.slice(0, 2).map((official) => official.id),
    player1TournamentPlayerId: dataset.tournamentPlayers[0]?.id ?? "",
    player2TournamentPlayerId: dataset.tournamentPlayers[1]?.id ?? "",
    status: "SCHEDULED",
  };
}

function toMatchRequest(match: Match): MatchRequest {
  return {
    divisionId: match.divisionId,
    stageId: match.stageId,
    groupId: match.groupId,
    matchNo: match.matchNo,
    scheduledAt: match.scheduledAt,
    courtName: match.courtName,
    refereeName: match.refereeName,
    refereeOfficialIds: match.refereeOfficialIds,
    durationMinutes: match.durationMinutes,
    maxSets: match.maxSets ?? 3,
    player1TournamentPlayerId: match.player1TournamentPlayerId,
    player2TournamentPlayerId: match.player2TournamentPlayerId,
    status: match.status.toUpperCase() as MatchRequest["status"],
  };
}
