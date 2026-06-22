import { SiteFrame } from "@/components/SiteFrame";
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
      <EmptyState
        title="순위 API 연결 대기"
        description="현재 백엔드에는 rankings 응답이 아직 없습니다. 조별 순위 계산 API가 준비되면 이 화면을 실제 순위 데이터에 연결합니다."
      />
    </SiteFrame>
  );
}
