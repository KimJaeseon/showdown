import { SiteFrame } from "@/components/SiteFrame";
import { PlayersView } from "@/components/PublicViews";
import { EmptyState, ErrorState } from "@/components/StateViews";
import { getPublicTournament } from "@/lib/api";

export default async function PlayersPage({
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
        <ErrorState title="선수 목록을 불러오지 못했습니다" message={error instanceof Error ? error.message : "백엔드 API 상태를 확인하세요."} />
      </SiteFrame>
    );
  }
  if (!dataset) {
    return (
      <SiteFrame tournamentCode={tournamentCode}>
        <EmptyState title="대회를 찾을 수 없습니다" description="선수 목록을 표시할 대회가 없습니다." />
      </SiteFrame>
    );
  }

  return (
    <SiteFrame tournamentCode={tournamentCode}>
      <PlayersView dataset={dataset} />
    </SiteFrame>
  );
}
