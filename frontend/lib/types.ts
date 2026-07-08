export type TournamentStatus =
  | "draft"
  | "published"
  | "running"
  | "finished"
  | "archived";

export type DivisionCategory =
  | "male"
  | "female"
  | "mixed"
  | "youth"
  | "open"
  | "custom";

export type StageType = "round_robin" | "knockout" | "placement";
export type MatchStatus = "scheduled" | "running" | "completed" | "cancelled" | "walkover";
export type MatchEndReason = "normal" | "giving_up" | "default_loss" | "bye";
export type MatchSide = "player1" | "player2";

export interface Tournament {
  id: string;
  code: string;
  name: string;
  location: string;
  startDate: string;
  endDate: string;
  timezone: string;
  status: TournamentStatus;
}

export interface Official {
  id: string;
  tournamentId: string;
  name: string;
  shortCode?: string;
  roleName: string;
  active: boolean;
}

export interface Division {
  id: string;
  tournamentId: string;
  name: string;
  code: string;
  category: DivisionCategory;
  sortOrder: number;
  isActive: boolean;
}

export interface Player {
  id: string;
  displayName: string;
  familyName?: string;
  givenName?: string;
  countryCode?: string;
}

export interface TournamentPlayer {
  id: string;
  tournamentId: string;
  divisionId: string;
  playerId: string;
  seedNo?: number;
  entryNo: number;
  displayNameOverride?: string;
  clubName?: string;
  status: "active" | "withdrawn" | "disqualified";
}

export interface Stage {
  id: string;
  tournamentId: string;
  divisionId: string;
  name: string;
  stageType: StageType;
  sortOrder: number;
}

export interface Group {
  id: string;
  tournamentId: string;
  divisionId: string;
  stageId: string;
  code: string;
  name: string;
  groupType: "league" | "knockout" | "placement";
  sortOrder: number;
}

export interface MatchSet {
  id: string;
  matchId: string;
  setNo: number;
  player1Score: number;
  player2Score: number;
  winnerSide: MatchSide;
  isTiebreak?: boolean;
}

export interface Match {
  id: string;
  tournamentId: string;
  divisionId: string;
  stageId: string;
  groupId?: string;
  matchNo: number;
  scheduledAt: string;
  durationMinutes: number;
  maxSets?: 1 | 3 | 5;
  courtName: string;
  refereeName?: string;
  refereeOfficialIds?: string[];
  status: MatchStatus;
  endReason: MatchEndReason;
  resultNote?: string;
  player1TournamentPlayerId: string;
  player2TournamentPlayerId: string;
  winnerTournamentPlayerId?: string;
  player1MatchPoints: number;
  player2MatchPoints: number;
  player1SetsWon: number;
  player2SetsWon: number;
  player1TotalPoints: number;
  player2TotalPoints: number;
  version: number;
  sets: MatchSet[];
}

export interface RankingSnapshot {
  id: string;
  tournamentId: string;
  divisionId: string;
  stageId: string;
  groupId?: string;
  tournamentPlayerId: string;
  rankNo: number;
  matchesPlayed: number;
  wins: number;
  losses: number;
  matchPoints: number;
  setsWon: number;
  setsLost: number;
  setDifference: number;
  pointsFor: number;
  pointsAgainst: number;
  pointDifference: number;
  tieBreakNote?: string;
}

export interface TournamentDataset {
  tournament: Tournament;
  divisions: Division[];
  players: Player[];
  tournamentPlayers: TournamentPlayer[];
  officials: Official[];
  stages: Stage[];
  groups: Group[];
  matches: Match[];
  rankings: RankingSnapshot[];
}

export interface ScoreSetInput {
  setNo: number;
  player1Score: number;
  player2Score: number;
}

export interface ScorePreview {
  winnerSide: MatchSide;
  player1SetsWon: number;
  player2SetsWon: number;
  player1TotalPoints: number;
  player2TotalPoints: number;
  winnerName: string;
}

export interface PlayerPortalStats {
  matchesPlayed: number;
  wins: number;
  losses: number;
  setsWon: number;
  setsLost: number;
  pointsFor: number;
  pointsAgainst: number;
}

export interface PlayerPortal {
  player: TournamentPlayer;
  nextMatch?: Match;
  scheduledMatches: Match[];
  completedMatches: Match[];
  stats: PlayerPortalStats;
}
