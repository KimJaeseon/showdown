import { SiteFrame } from "@/components/SiteFrame";
import { MatchesView } from "@/components/PublicViews";
import { EmptyState, ErrorState } from "@/components/StateViews";
import { getPublicTournament } from "@/lib/api";

export default async function MatchesPage({
  params,
}: {
  params: Promise<{ tournamentCode: string }>;
}) {
  const { tournamentCode } = await params;
  let dataset = null;
  try {
    dataset = await getPublicTournament(tournamentCode);
  } catch (error) {
    return (
      <SiteFrame tournamentCode={tournamentCode}>
        <ErrorState title="경기 일정을 불러오지 못했습니다" message={error instanceof Error ? error.message : "백엔드 API 상태를 확인하세요."} />
      </SiteFrame>
    );
  }
  if (!dataset) {
    return (
      <SiteFrame tournamentCode={tournamentCode}>
        <EmptyState title="대회를 찾을 수 없습니다" description="경기 일정을 표시할 대회가 없습니다." />
      </SiteFrame>
    );
  }

  return (
    <SiteFrame tournamentCode={tournamentCode}>
      <MatchesView dataset={dataset} />
    </SiteFrame>
  );
}
