import Link from "next/link";
import {
  describeMatch,
  formatDateRange,
  formatMatchDate,
  formatMatchTime,
  formatSets,
  getDivisionName,
  getGroupName,
  getPlayerMeta,
  getPlayerName,
  matchStatusLabel,
} from "@/lib/helpers";
import type { Match, RankingSnapshot, TournamentDataset } from "@/lib/types";

export function TournamentHome({ dataset }: { dataset: TournamentDataset }) {
  const { tournament, divisions, matches, tournamentPlayers } = dataset;
  const completedCount = matches.filter((match) => match.status === "completed").length;
  const scheduledCount = matches.filter((match) => match.status === "scheduled").length;

  return (
    <>
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">공개 조회</p>
        <h1 id="page-title">{tournament.name}</h1>
        <p className="lead">
          {formatDateRange(tournament.startDate, tournament.endDate)}. {tournament.location}. 현재
          상태는 {tournament.status}입니다.
        </p>
      </section>

      <section aria-labelledby="summary-title">
        <h2 id="summary-title">대회 요약</h2>
        <div className="grid">
          <SummaryItem title="부문" value={`${divisions.length}개`} />
          <SummaryItem title="참가 선수" value={`${tournamentPlayers.length}명`} />
          <SummaryItem title="완료 경기" value={`${completedCount}경기`} />
          <SummaryItem title="예정 경기" value={`${scheduledCount}경기`} />
        </div>
      </section>

      <section aria-labelledby="menu-title">
        <h2 id="menu-title">주요 메뉴</h2>
        <div className="grid">
          <MenuLink href={`/tournaments/${tournament.code}/players`} title="선수 목록" />
          <MenuLink href={`/tournaments/${tournament.code}/matches`} title="경기 일정" />
          <MenuLink href={`/tournaments/${tournament.code}/groups`} title="조별 순위" />
          <MenuLink href={`/tournaments/${tournament.code}/rankings`} title="최종 순위" />
        </div>
      </section>
    </>
  );
}

function SummaryItem({ title, value }: { title: string; value: string }) {
  return (
    <div className="panel">
      <h3>{title}</h3>
      <p>{value}</p>
    </div>
  );
}

function MenuLink({ href, title }: { href: string; title: string }) {
  return (
    <Link className="item-card" href={href}>
      <strong>{title}</strong>
      <span>선택하여 상세 정보를 확인합니다.</span>
    </Link>
  );
}

export function PlayersView({ dataset }: { dataset: TournamentDataset }) {
  const { divisions, players, tournamentPlayers } = dataset;

  return (
    <>
      <PageIntro title="선수 목록" description="부문별 참가 번호, 선수 이름, 국가, 시드 번호를 제공합니다." />
      {divisions.map((division) => {
        const entries = tournamentPlayers
          .filter((entry) => entry.divisionId === division.id)
          .sort((a, b) => a.entryNo - b.entryNo);

        return (
          <section key={division.id} aria-labelledby={`division-${division.id}`}>
            <h2 id={`division-${division.id}`}>{division.name}</h2>
            <ol className="sentence-list">
              {entries.map((entry) => {
                const { player } = getPlayerMeta(entry.id, tournamentPlayers, players);
                const name = entry.displayNameOverride ?? player?.displayName ?? "미정 선수";
                return (
                  <li key={entry.id}>
                    참가 번호 {entry.entryNo}번. 선수 {name}. 국가 {player?.countryCode ?? "미지정"}.
                    시드 번호 {entry.seedNo ?? "없음"}. 소속 {entry.clubName ?? "미지정"}.
                  </li>
                );
              })}
            </ol>
          </section>
        );
      })}
    </>
  );
}

export function MatchesView({ dataset }: { dataset: TournamentDataset }) {
  const grouped = groupMatchesByDate(dataset.matches);

  return (
    <>
      <PageIntro title="경기 일정" description="날짜별 경기 번호, 시간, 테이블, 선수, 결과와 세트 점수를 제공합니다." />
      <section className="screen-reader-summary" aria-labelledby="match-summary-title">
        <h2 id="match-summary-title">스크린리더용 경기 설명</h2>
        <ol className="sentence-list">
          {dataset.matches.map((match) => (
            <li key={match.id}>
              {describeMatch(match, dataset.tournamentPlayers, dataset.players, dataset.groups)}
            </li>
          ))}
        </ol>
      </section>

      {Object.entries(grouped).map(([date, matches]) => (
        <section key={date} aria-labelledby={`date-${date}`}>
          <h2 id={`date-${date}`}>{date}</h2>
          <MatchTable matches={matches} dataset={dataset} caption={`${date} 경기 일정`} />
        </section>
      ))}
    </>
  );
}

function MatchTable({ matches, dataset, caption }: { matches: Match[]; dataset: TournamentDataset; caption: string }) {
  return (
    <div className="table-wrap">
      <table>
        <caption>{caption}</caption>
        <thead>
          <tr>
            <th scope="col">경기</th>
            <th scope="col">시간</th>
            <th scope="col">테이블</th>
            <th scope="col">그룹</th>
            <th scope="col">선수</th>
            <th scope="col">상태와 결과</th>
            <th scope="col">세트 점수</th>
          </tr>
        </thead>
        <tbody>
          {matches.map((match) => (
            <tr key={match.id}>
              <td>{match.matchNo}번</td>
              <td>{formatMatchTime(match.scheduledAt)}</td>
              <td>{match.courtName}</td>
              <td>{getGroupName(match.groupId, dataset.groups)}</td>
              <td>
                {getPlayerName(match.player1TournamentPlayerId, dataset.tournamentPlayers, dataset.players)}
                <br />
                대{" "}
                {getPlayerName(match.player2TournamentPlayerId, dataset.tournamentPlayers, dataset.players)}
              </td>
              <td>
                <span className={`status ${match.status}`}>{matchStatusLabel(match.status)}</span>
                {match.winnerTournamentPlayerId ? (
                  <p>{getPlayerName(match.winnerTournamentPlayerId, dataset.tournamentPlayers, dataset.players)} 승리</p>
                ) : null}
              </td>
              <td>{formatSets(match.sets)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function GroupsView({ dataset }: { dataset: TournamentDataset }) {
  return (
    <>
      <PageIntro title="조별 순위" description="부문과 단계별 그룹 순위를 승점, 경기 수, 세트 득실, 점수 득실과 함께 제공합니다." />
      {dataset.groups.map((group) => {
        const rankings = dataset.rankings
          .filter((ranking) => ranking.groupId === group.id)
          .sort((a, b) => a.rankNo - b.rankNo);

        return (
          <RankingSection
            key={group.id}
            title={`${getDivisionName(group.divisionId, dataset.divisions)} ${group.name}`}
            rankings={rankings}
            dataset={dataset}
          />
        );
      })}
    </>
  );
}

export function RankingsView({ dataset }: { dataset: TournamentDataset }) {
  return (
    <>
      <PageIntro title="최종 순위" description="현재 계산된 순위를 부문별로 제공합니다. MVP 샘플에서는 조별 순위 스냅샷을 최종 순위 형식으로 표시합니다." />
      {dataset.divisions.map((division) => {
        const rankings = dataset.rankings
          .filter((ranking) => ranking.divisionId === division.id)
          .sort((a, b) => a.rankNo - b.rankNo);

        return (
          <RankingSection
            key={division.id}
            title={`${division.name} 최종 순위`}
            rankings={rankings}
            dataset={dataset}
            compact
          />
        );
      })}
    </>
  );
}

function RankingSection({
  title,
  rankings,
  dataset,
  compact = false,
}: {
  title: string;
  rankings: RankingSnapshot[];
  dataset: TournamentDataset;
  compact?: boolean;
}) {
  return (
    <section aria-labelledby={`ranking-${title}`}>
      <h2 id={`ranking-${title}`}>{title}</h2>
      <ol className="sentence-list screen-reader-summary">
        {rankings.map((ranking) => (
          <li key={ranking.id}>
            {ranking.rankNo}위. 선수{" "}
            {getPlayerName(ranking.tournamentPlayerId, dataset.tournamentPlayers, dataset.players)}. 승점{" "}
            {ranking.matchPoints}점. 경기 수 {ranking.matchesPlayed}. 세트 득실 {ranking.setDifference}. 점수 득실{" "}
            {ranking.pointDifference}. {ranking.tieBreakNote ?? ""}
          </li>
        ))}
      </ol>
      <div className="table-wrap" aria-hidden={false}>
        <table>
          <caption>{title}</caption>
          <thead>
            <tr>
              <th scope="col">순위</th>
              <th scope="col">선수</th>
              {!compact ? <th scope="col">승점</th> : null}
              <th scope="col">경기</th>
              <th scope="col">승패</th>
              <th scope="col">세트 득실</th>
              <th scope="col">점수 득실</th>
            </tr>
          </thead>
          <tbody>
            {rankings.map((ranking) => (
              <tr key={ranking.id}>
                <td>{ranking.rankNo}</td>
                <td>{getPlayerName(ranking.tournamentPlayerId, dataset.tournamentPlayers, dataset.players)}</td>
                {!compact ? <td>{ranking.matchPoints}</td> : null}
                <td>{ranking.matchesPlayed}</td>
                <td>
                  {ranking.wins}승 {ranking.losses}패
                </td>
                <td>{ranking.setDifference}</td>
                <td>{ranking.pointDifference}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function PageIntro({ title, description }: { title: string; description: string }) {
  return (
    <section className="hero" aria-labelledby="page-title">
      <p className="eyebrow">공개 조회</p>
      <h1 id="page-title">{title}</h1>
      <p className="lead">{description}</p>
    </section>
  );
}

function groupMatchesByDate(matches: Match[]) {
  return matches.reduce<Record<string, Match[]>>((acc, match) => {
    const date = formatMatchDate(match.scheduledAt);
    acc[date] = [...(acc[date] ?? []), match];
    return acc;
  }, {});
}
