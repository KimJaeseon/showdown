import type {
  Division,
  Group,
  Match,
  MatchSet,
  MatchStatus,
  Player,
  ScorePreview,
  ScoreSetInput,
  TournamentPlayer,
} from "./types";

export function formatDateRange(startDate: string, endDate: string) {
  const formatter = new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
  const start = formatter.format(new Date(`${startDate}T00:00:00+09:00`));
  const end = formatter.format(new Date(`${endDate}T00:00:00+09:00`));
  return startDate === endDate ? start : `${start}부터 ${end}까지`;
}

export function formatMatchTime(iso: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(iso));
}

export function formatMatchDate(iso: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  }).format(new Date(iso));
}

export function getPlayerName(
  tournamentPlayerId: string,
  tournamentPlayers: TournamentPlayer[],
  players: Player[],
) {
  const tournamentPlayer = tournamentPlayers.find((item) => item.id === tournamentPlayerId);
  const player = players.find((item) => item.id === tournamentPlayer?.playerId);
  return tournamentPlayer?.displayNameOverride ?? player?.displayName ?? "미정 선수";
}

export function getPlayerMeta(
  tournamentPlayerId: string,
  tournamentPlayers: TournamentPlayer[],
  players: Player[],
) {
  const tournamentPlayer = tournamentPlayers.find((item) => item.id === tournamentPlayerId);
  const player = players.find((item) => item.id === tournamentPlayer?.playerId);
  return { tournamentPlayer, player };
}

export function getDivisionName(divisionId: string, divisions: Division[]) {
  return divisions.find((division) => division.id === divisionId)?.name ?? "미지정 부문";
}

export function getGroupName(groupId: string | undefined, groups: Group[]) {
  if (!groupId) return "미지정 그룹";
  return groups.find((group) => group.id === groupId)?.name ?? "미지정 그룹";
}

export function matchStatusLabel(status: MatchStatus) {
  const labels: Record<MatchStatus, string> = {
    scheduled: "예정",
    running: "진행 중",
    completed: "완료",
    cancelled: "취소",
    walkover: "부전승",
  };
  return labels[status];
}

export function formatSets(sets: MatchSet[]) {
  if (sets.length === 0) return "세트 점수 없음";
  return sets
    .sort((a, b) => a.setNo - b.setNo)
    .map((set) => `${set.player1Score}대${set.player2Score}`)
    .join(", ");
}

export function describeMatch(
  match: Match,
  tournamentPlayers: TournamentPlayer[],
  players: Player[],
  groups: Group[],
) {
  const player1 = getPlayerName(match.player1TournamentPlayerId, tournamentPlayers, players);
  const player2 = getPlayerName(match.player2TournamentPlayerId, tournamentPlayers, players);
  const result =
    match.status === "completed" && match.winnerTournamentPlayerId
      ? `${getPlayerName(match.winnerTournamentPlayerId, tournamentPlayers, players)} 승리`
      : matchStatusLabel(match.status);

  return `경기 ${match.matchNo}번. ${formatMatchTime(match.scheduledAt)}. ${match.courtName}. ${getGroupName(
    match.groupId,
    groups,
  )}. 선수 1은 ${player1}. 선수 2는 ${player2}. 결과는 ${result}. 세트 점수는 ${formatSets(
    match.sets,
  )}.`;
}

export function calculateScorePreview(
  sets: ScoreSetInput[],
  player1Name: string,
  player2Name: string,
): ScorePreview | null {
  if (sets.length === 0) return null;
  let player1SetsWon = 0;
  let player2SetsWon = 0;
  let player1TotalPoints = 0;
  let player2TotalPoints = 0;

  for (const set of sets) {
    player1TotalPoints += set.player1Score;
    player2TotalPoints += set.player2Score;
    if (set.player1Score > set.player2Score) player1SetsWon += 1;
    if (set.player2Score > set.player1Score) player2SetsWon += 1;
  }

  const winnerSide = player1SetsWon >= player2SetsWon ? "player1" : "player2";

  return {
    winnerSide,
    player1SetsWon,
    player2SetsWon,
    player1TotalPoints,
    player2TotalPoints,
    winnerName: winnerSide === "player1" ? player1Name : player2Name,
  };
}
