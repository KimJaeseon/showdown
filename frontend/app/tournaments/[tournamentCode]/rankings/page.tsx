import { SiteFrame } from "@/components/SiteFrame";
import { RankingsView } from "@/components/PublicViews";
import { EmptyState, ErrorState } from "@/components/StateViews";
import { getPublicTournament } from "@/lib/api";

export default async function RankingsPage({
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
        <ErrorState title="순위 화면을 준비하지 못했습니다" message={error instanceof Error ? error.message : "백엔드 API 상태를 확인하세요."} />
      </SiteFrame>
    );
  }
  if (!dataset) {
    return (
      <SiteFrame tournamentCode={tournamentCode}>
        <EmptyState title="대회를 찾을 수 없습니다" description="순위를 표시할 대회가 없습니다." />
      </SiteFrame>
    );
  }

  return (
    <SiteFrame tournamentCode={tournamentCode}>
      <RankingsView dataset={dataset} />
    </SiteFrame>
  );
}
