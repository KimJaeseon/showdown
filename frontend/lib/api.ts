import type {
  Division,
  DivisionCategory,
  Group,
  Match,
  MatchEndReason,
  MatchSet,
  MatchSide,
  MatchStatus,
  Official,
  Player,
  PlayerPortal,
  RankingSnapshot,
  ScoreSetInput,
  Stage,
  StageType,
  Tournament,
  TournamentDataset,
  TournamentPlayer,
  TournamentStatus,
} from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

export interface BasicAuthSession {
  username: string;
  password: string;
  role: "ADMIN" | "REFEREE" | "PLAYER";
}

interface BackendTournament {
  id: string;
  code: string;
  name: string;
  location?: string;
  startDate: string;
  endDate: string;
  timezone: string;
  status: string;
  defaultLanguage: string;
}

interface BackendDivision {
  id: string;
  tournamentId: string;
  name: string;
  code: string;
  category: string;
  sortOrder: number;
  active: boolean;
}

interface BackendTournamentPlayer {
  id: string;
  tournamentId: string;
  divisionId: string;
  playerId: string;
  displayName: string;
  countryCode?: string;
  seedNo?: number;
  entryNo?: number;
  displayNameOverride?: string;
  clubName?: string;
  status: string;
}

interface BackendOfficial {
  id: string;
  tournamentId: string;
  name: string;
  shortCode?: string;
  roleName: string;
  active: boolean;
}

interface BackendStage {
  id: string;
  tournamentId: string;
  divisionId: string;
  name: string;
  stageType: string;
  sortOrder: number;
}

interface BackendGroup {
  id: string;
  tournamentId: string;
  divisionId: string;
  stageId: string;
  code: string;
  name: string;
  groupType: string;
  sortOrder: number;
}

interface BackendMatchSet {
  id: string;
  setNo: number;
  player1Score: number;
  player2Score: number;
  winnerSide: string;
}

interface BackendMatch {
  id: string;
  tournamentId: string;
  divisionId: string;
  stageId: string;
  groupId?: string;
  matchNo: number;
  scheduledAt?: string;
  courtName?: string;
  refereeName?: string;
  refereeOfficialIds?: string[];
  durationMinutes?: number;
  maxSets?: number;
  player1TournamentPlayerId: string;
  player1Name: string;
  player2TournamentPlayerId: string;
  player2Name: string;
  winnerTournamentPlayerId?: string;
  status: string;
  player1SetsWon: number;
  player2SetsWon: number;
  player1TotalPoints: number;
  player2TotalPoints: number;
  version: number;
  endReason?: string;
  resultNote?: string;
  sets: BackendMatchSet[];
}

interface BackendPlayerPortal {
  player: BackendTournamentPlayer;
  nextMatch?: BackendMatch;
  scheduledMatches: BackendMatch[];
  completedMatches: BackendMatch[];
  stats: PlayerPortal["stats"];
}

export interface TournamentRequest {
  code: string;
  name: string;
  location?: string;
  startDate: string;
  endDate: string;
  timezone: string;
  status: "DRAFT" | "PUBLISHED" | "RUNNING" | "FINISHED" | "ARCHIVED";
  defaultLanguage: string;
}

export interface DivisionRequest {
  name: string;
  code: string;
  category: "MALE" | "FEMALE" | "MIXED" | "YOUTH" | "OPEN" | "CUSTOM";
  sortOrder?: number;
  active?: boolean;
}

export interface TournamentPlayerRequest {
  displayName: string;
  countryCode?: string;
  divisionId: string;
  seedNo?: number;
  entryNo?: number;
  displayNameOverride?: string;
  clubName?: string;
  status?: "ACTIVE" | "WITHDRAWN" | "DISQUALIFIED";
}

export interface GroupRequest {
  divisionId: string;
  stageId: string;
  code: string;
  name: string;
  groupType: "LEAGUE" | "KNOCKOUT" | "PLACEMENT";
  sortOrder?: number;
}

export interface OfficialRequest {
  name: string;
  shortCode?: string;
  roleName: string;
  active?: boolean;
}

export interface StageRequest {
  divisionId: string;
  name: string;
  stageType: "ROUND_ROBIN" | "KNOCKOUT" | "PLACEMENT";
  sortOrder?: number;
}

export interface MatchRequest {
  divisionId: string;
  stageId: string;
  groupId?: string;
  matchNo: number;
  scheduledAt?: string;
  courtName?: string;
  refereeName?: string;
  refereeOfficialIds?: string[];
  durationMinutes?: number;
  maxSets?: 1 | 3 | 5;
  player1TournamentPlayerId: string;
  player2TournamentPlayerId: string;
  status?: "SCHEDULED" | "RUNNING" | "COMPLETED" | "CANCELLED" | "WALKOVER";
}

export interface GroupMemberResponse {
  id: string;
  groupId: string;
  tournamentPlayerId: string;
  playerName: string;
  slotNo: number;
  sourceRule?: string;
}

export interface RoundRobinPreview {
  expectedMatchCount: number;
  createdMatchCount: number;
  matches: Array<{ player1Id: string; player1Name: string; player2Id: string; player2Name: string }>;
}

export function authHeader(auth: BasicAuthSession) {
  // 백엔드는 현재 Basic Auth를 사용한다. 인증 정보는 sessionStorage에만 두고,
  // 실제 요청 직전에 Authorization 헤더로 변환한다.
  return `Basic ${btoa(`${auth.username}:${auth.password}`)}`;
}

export async function verifyAuth(auth: BasicAuthSession) {
  return request<{ username: string; authorities: string[] }>("/api/auth/me", { headers: withAuth(auth) });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
    cache: "no-store",
  });

  if (!response.ok) {
    let message = `API 요청에 실패했습니다. 상태 코드: ${response.status}`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) message = body.message;
    } catch {
      // JSON 오류 본문이 아닌 경우 기본 메시지를 그대로 사용한다.
    }
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function withAuth(auth: BasicAuthSession): HeadersInit {
  return { Authorization: authHeader(auth) };
}

export async function getPublicTournament(tournamentCode: string) {
  try {
    const [tournament, divisions, tournamentPlayers, groups, matches, rankings] = await Promise.all([
      request<BackendTournament>(`/api/public/tournaments/${tournamentCode}`),
      request<BackendDivision[]>(`/api/public/tournaments/${tournamentCode}/divisions`),
      request<BackendTournamentPlayer[]>(`/api/public/tournaments/${tournamentCode}/players`),
      request<BackendGroup[]>(`/api/public/tournaments/${tournamentCode}/groups`),
      request<BackendMatch[]>(`/api/public/tournaments/${tournamentCode}/matches`),
      request<RankingSnapshot[]>(`/api/public/tournaments/${tournamentCode}/rankings`),
    ]);

    return toDataset({
      tournament,
      divisions,
      tournamentPlayers,
      officials: [],
      stages: inferStages(groups, matches),
      groups,
      matches,
      rankings,
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export async function getAdminDataset(auth: BasicAuthSession, tournamentId?: string) {
  const tournaments = await request<BackendTournament[]>("/api/admin/tournaments", { headers: withAuth(auth) });
  const tournament = tournamentId
    ? tournaments.find((item) => item.id === tournamentId)
    : tournaments[0];

  if (!tournament) {
    return emptyDataset();
  }

  const [divisions, tournamentPlayers, officials, stages, groups, matches, rankings] = await Promise.all([
    request<BackendDivision[]>(`/api/admin/tournaments/${tournament.id}/divisions`, { headers: withAuth(auth) }),
    request<BackendTournamentPlayer[]>(`/api/admin/tournaments/${tournament.id}/players`, { headers: withAuth(auth) }),
    request<BackendOfficial[]>(`/api/admin/tournaments/${tournament.id}/officials`, { headers: withAuth(auth) }),
    request<BackendStage[]>(`/api/admin/tournaments/${tournament.id}/stages`, { headers: withAuth(auth) }),
    request<BackendGroup[]>(`/api/admin/tournaments/${tournament.id}/groups`, { headers: withAuth(auth) }),
    request<BackendMatch[]>(`/api/admin/tournaments/${tournament.id}/matches`, { headers: withAuth(auth) }),
    request<RankingSnapshot[]>(`/api/public/tournaments/${tournament.code}/rankings`),
  ]);

  return toDataset({
    tournament,
    divisions,
    tournamentPlayers,
    officials,
    stages,
    groups,
    matches,
    rankings,
  });
}

export async function createTournament(auth: BasicAuthSession, body: TournamentRequest) {
  return mapTournament(await request<BackendTournament>("/api/admin/tournaments", {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateTournament(auth: BasicAuthSession, tournamentId: string, body: TournamentRequest) {
  return mapTournament(await request<BackendTournament>(`/api/admin/tournaments/${tournamentId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function createDivision(auth: BasicAuthSession, tournamentId: string, body: DivisionRequest) {
  return mapDivision(await request<BackendDivision>(`/api/admin/tournaments/${tournamentId}/divisions`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateDivision(auth: BasicAuthSession, divisionId: string, body: DivisionRequest) {
  return mapDivision(await request<BackendDivision>(`/api/admin/divisions/${divisionId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteDivision(auth: BasicAuthSession, divisionId: string) {
  await request<void>(`/api/admin/divisions/${divisionId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function createTournamentPlayer(auth: BasicAuthSession, tournamentId: string, body: TournamentPlayerRequest) {
  return mapTournamentPlayer(await request<BackendTournamentPlayer>(`/api/admin/tournaments/${tournamentId}/players`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateTournamentPlayer(auth: BasicAuthSession, tournamentPlayerId: string, body: TournamentPlayerRequest) {
  return mapTournamentPlayer(await request<BackendTournamentPlayer>(`/api/admin/players/${tournamentPlayerId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteTournamentPlayer(auth: BasicAuthSession, tournamentPlayerId: string) {
  await request<void>(`/api/admin/players/${tournamentPlayerId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function createOfficial(auth: BasicAuthSession, tournamentId: string, body: OfficialRequest) {
  return mapOfficial(await request<BackendOfficial>(`/api/admin/tournaments/${tournamentId}/officials`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateOfficial(auth: BasicAuthSession, officialId: string, body: OfficialRequest) {
  return mapOfficial(await request<BackendOfficial>(`/api/admin/officials/${officialId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteOfficial(auth: BasicAuthSession, officialId: string) {
  await request<void>(`/api/admin/officials/${officialId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function createStage(auth: BasicAuthSession, tournamentId: string, body: StageRequest) {
  return mapStage(await request<BackendStage>(`/api/admin/tournaments/${tournamentId}/stages`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateStage(auth: BasicAuthSession, stageId: string, body: StageRequest) {
  return mapStage(await request<BackendStage>(`/api/admin/stages/${stageId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteStage(auth: BasicAuthSession, stageId: string) {
  await request<void>(`/api/admin/stages/${stageId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function createGroup(auth: BasicAuthSession, tournamentId: string, body: GroupRequest) {
  return mapGroup(await request<BackendGroup>(`/api/admin/tournaments/${tournamentId}/groups`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateGroup(auth: BasicAuthSession, groupId: string, body: GroupRequest) {
  return mapGroup(await request<BackendGroup>(`/api/admin/groups/${groupId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteGroup(auth: BasicAuthSession, groupId: string) {
  await request<void>(`/api/admin/groups/${groupId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function getGroupMembers(auth: BasicAuthSession, groupId: string) {
  return request<GroupMemberResponse[]>(`/api/admin/groups/${groupId}/members`, { headers: withAuth(auth) });
}

export async function addGroupMember(auth: BasicAuthSession, groupId: string, tournamentPlayerId: string, slotNo: number) {
  return request<GroupMemberResponse>(`/api/admin/groups/${groupId}/members`, {
    method: "POST", headers: withAuth(auth), body: JSON.stringify({ tournamentPlayerId, slotNo }),
  });
}

export async function removeGroupMember(auth: BasicAuthSession, groupId: string, memberId: string) {
  await request<void>(`/api/admin/groups/${groupId}/members/${memberId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function previewRoundRobin(auth: BasicAuthSession, groupId: string) {
  return request<RoundRobinPreview>(`/api/admin/groups/${groupId}/round-robin/preview`, { headers: withAuth(auth) });
}

export async function generateRoundRobin(auth: BasicAuthSession, groupId: string, body: {
  startAt: string; matchDurationMinutes: number; courtNames: string[]; officialIds: string[];
}) {
  return request<RoundRobinPreview>(`/api/admin/groups/${groupId}/round-robin`, {
    method: "POST", headers: withAuth(auth), body: JSON.stringify(body),
  });
}

export async function createMatch(auth: BasicAuthSession, tournamentId: string, body: MatchRequest) {
  return mapMatch(await request<BackendMatch>(`/api/admin/tournaments/${tournamentId}/matches`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function updateMatch(auth: BasicAuthSession, matchId: string, body: MatchRequest) {
  return mapMatch(await request<BackendMatch>(`/api/admin/matches/${matchId}`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function deleteMatch(auth: BasicAuthSession, matchId: string) {
  await request<void>(`/api/admin/matches/${matchId}`, { method: "DELETE", headers: withAuth(auth) });
}

export async function saveMatchSets(auth: BasicAuthSession, matchId: string, sets: ScoreSetInput[], version: number) {
  return mapMatch(await request<BackendMatch>(`/api/scoring/matches/${matchId}/sets`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify({ version, sets }),
  }));
}

export async function saveMatchSetDraft(auth: BasicAuthSession, matchId: string, sets: ScoreSetInput[], version: number) {
  return mapMatch(await request<BackendMatch>(`/api/scoring/matches/${matchId}/draft`, {
    method: "PUT",
    headers: withAuth(auth),
    body: JSON.stringify({ version, sets }),
  }));
}

export async function confirmMatchResult(auth: BasicAuthSession, matchId: string, version: number, changeReason?: string) {
  return mapMatch(await request<BackendMatch>(`/api/scoring/matches/${matchId}/confirm`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify({ version, changeReason }),
  }));
}

export async function finishMatchSpecial(
  auth: BasicAuthSession,
  matchId: string,
  body: { version: number; reason: "GIVING_UP" | "DEFAULT_LOSS" | "BYE"; winnerSide: "PLAYER1" | "PLAYER2"; note?: string },
) {
  return mapMatch(await request<BackendMatch>(`/api/scoring/matches/${matchId}/finish-special`, {
    method: "POST",
    headers: withAuth(auth),
    body: JSON.stringify(body),
  }));
}

export async function getPlayerPortal(auth: BasicAuthSession): Promise<PlayerPortal> {
  const portal = await request<BackendPlayerPortal>("/api/player/me", { headers: withAuth(auth) });
  return {
    player: mapTournamentPlayer(portal.player),
    nextMatch: portal.nextMatch ? mapMatch(portal.nextMatch) : undefined,
    scheduledMatches: portal.scheduledMatches.map(mapMatch),
    completedMatches: portal.completedMatches.map(mapMatch),
    stats: portal.stats,
  };
}

function toDataset(source: {
  tournament: BackendTournament;
  divisions: BackendDivision[];
  tournamentPlayers: BackendTournamentPlayer[];
  officials: BackendOfficial[];
  stages: BackendStage[];
  groups: BackendGroup[];
  matches: BackendMatch[];
  rankings: RankingSnapshot[];
}): TournamentDataset {
  const tournamentPlayers = source.tournamentPlayers.map(mapTournamentPlayer);
  const playerMap = new Map<string, Player>();
  source.tournamentPlayers.forEach((entry) => {
    playerMap.set(entry.playerId, {
      id: entry.playerId,
      displayName: entry.displayName,
      countryCode: entry.countryCode,
    });
  });

  // 경기 응답은 playerName을 포함하지만 별도 선수 목록에 없을 수 있다.
  // 화면 헬퍼가 tournamentPlayers를 기준으로 이름을 찾으므로 여기에서 누락분을 보강한다.
  source.matches.forEach((match) => {
    ensureSyntheticPlayer(playerMap, tournamentPlayers, match.tournamentId, match.divisionId, match.player1TournamentPlayerId, match.player1Name);
    ensureSyntheticPlayer(playerMap, tournamentPlayers, match.tournamentId, match.divisionId, match.player2TournamentPlayerId, match.player2Name);
  });

  return {
    tournament: mapTournament(source.tournament),
    divisions: source.divisions.map(mapDivision),
    players: [...playerMap.values()],
    tournamentPlayers,
    officials: source.officials.map(mapOfficial),
    stages: source.stages.map(mapStage),
    groups: source.groups.map(mapGroup),
    matches: source.matches.map(mapMatch),
    rankings: source.rankings,
  };
}

function emptyDataset(): TournamentDataset {
  return {
    tournament: {
      id: "",
      code: "",
      name: "등록된 대회가 없습니다",
      location: "",
      startDate: new Date().toISOString().slice(0, 10),
      endDate: new Date().toISOString().slice(0, 10),
      timezone: "Asia/Seoul",
      status: "draft",
    },
    divisions: [],
    players: [],
    tournamentPlayers: [],
    officials: [],
    stages: [],
    groups: [],
    matches: [],
    rankings: [],
  };
}

function mapTournament(item: BackendTournament): Tournament {
  return {
    id: item.id,
    code: item.code,
    name: item.name,
    location: item.location ?? "",
    startDate: item.startDate,
    endDate: item.endDate,
    timezone: item.timezone,
    status: lowerEnum<TournamentStatus>(item.status),
  };
}

function mapDivision(item: BackendDivision): Division {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    name: item.name,
    code: item.code,
    category: lowerEnum<DivisionCategory>(item.category),
    sortOrder: item.sortOrder,
    isActive: item.active,
  };
}

function mapTournamentPlayer(item: BackendTournamentPlayer): TournamentPlayer {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    divisionId: item.divisionId,
    playerId: item.playerId,
    seedNo: item.seedNo,
    entryNo: item.entryNo ?? 0,
    displayNameOverride: item.displayNameOverride,
    clubName: item.clubName,
    status: lowerEnum<TournamentPlayer["status"]>(item.status),
  };
}

function mapOfficial(item: BackendOfficial): Official {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    name: item.name,
    shortCode: item.shortCode,
    roleName: item.roleName,
    active: item.active,
  };
}

function mapStage(item: BackendStage): Stage {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    divisionId: item.divisionId,
    name: item.name,
    stageType: lowerEnum<StageType>(item.stageType),
    sortOrder: item.sortOrder,
  };
}

function mapGroup(item: BackendGroup): Group {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    divisionId: item.divisionId,
    stageId: item.stageId,
    code: item.code,
    name: item.name,
    groupType: lowerEnum<Group["groupType"]>(item.groupType),
    sortOrder: item.sortOrder,
  };
}

function mapMatch(item: BackendMatch): Match {
  return {
    id: item.id,
    tournamentId: item.tournamentId,
    divisionId: item.divisionId,
    stageId: item.stageId,
    groupId: item.groupId,
    matchNo: item.matchNo,
    scheduledAt: item.scheduledAt ?? new Date().toISOString(),
    durationMinutes: item.durationMinutes ?? 30,
    courtName: item.courtName ?? "미정",
    refereeName: item.refereeName,
    refereeOfficialIds: item.refereeOfficialIds,
    maxSets: (item.maxSets ?? 3) as 1 | 3 | 5,
    status: lowerEnum<MatchStatus>(item.status),
    endReason: lowerEnum<MatchEndReason>(item.endReason ?? "NORMAL"),
    resultNote: item.resultNote,
    player1TournamentPlayerId: item.player1TournamentPlayerId,
    player2TournamentPlayerId: item.player2TournamentPlayerId,
    winnerTournamentPlayerId: item.winnerTournamentPlayerId,
    player1MatchPoints: item.winnerTournamentPlayerId === item.player1TournamentPlayerId ? 1 : 0,
    player2MatchPoints: item.winnerTournamentPlayerId === item.player2TournamentPlayerId ? 1 : 0,
    player1SetsWon: item.player1SetsWon,
    player2SetsWon: item.player2SetsWon,
    player1TotalPoints: item.player1TotalPoints,
    player2TotalPoints: item.player2TotalPoints,
    version: item.version,
    sets: item.sets.map((set) => mapMatchSet(item.id, set)),
  };
}

function mapMatchSet(matchId: string, item: BackendMatchSet): MatchSet {
  return {
    id: item.id,
    matchId,
    setNo: item.setNo,
    player1Score: item.player1Score,
    player2Score: item.player2Score,
    winnerSide: lowerEnum<MatchSide>(item.winnerSide),
  };
}

function lowerEnum<T extends string>(value: string): T {
  return value.toLowerCase() as T;
}

function ensureSyntheticPlayer(
  playerMap: Map<string, Player>,
  tournamentPlayers: TournamentPlayer[],
  tournamentId: string,
  divisionId: string,
  tournamentPlayerId: string,
  displayName: string,
) {
  if (!tournamentPlayers.some((entry) => entry.id === tournamentPlayerId)) {
    const playerId = `player-for-${tournamentPlayerId}`;
    playerMap.set(playerId, { id: playerId, displayName });
    tournamentPlayers.push({
      id: tournamentPlayerId,
      tournamentId,
      divisionId,
      playerId,
      entryNo: 0,
      status: "active",
    });
  }
}

function inferStages(groups: BackendGroup[], matches: BackendMatch[]): BackendStage[] {
  const stageIds = new Map<string, BackendStage>();
  groups.forEach((group) => {
    stageIds.set(group.stageId, {
      id: group.stageId,
      tournamentId: group.tournamentId,
      divisionId: group.divisionId,
      name: "단계",
      stageType: "ROUND_ROBIN",
      sortOrder: 0,
    });
  });
  matches.forEach((match) => {
    stageIds.set(match.stageId, {
      id: match.stageId,
      tournamentId: match.tournamentId,
      divisionId: match.divisionId,
      name: "단계",
      stageType: "ROUND_ROBIN",
      sortOrder: 0,
    });
  });
  return [...stageIds.values()];
}
