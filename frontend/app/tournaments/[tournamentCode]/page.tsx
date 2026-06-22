import { SiteFrame } from "@/components/SiteFrame";
import { TournamentHome } from "@/components/PublicViews";
import { EmptyState, ErrorState } from "@/components/StateViews";
import { getPublicTournament } from "@/lib/api";

export default async function TournamentPage({
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
        <ErrorState title="대회 정보를 불러오지 못했습니다" message={error instanceof Error ? error.message : "백엔드 API 상태를 확인하세요."} />
      </SiteFrame>
    );
  }
  if (!dataset) {
    return (
      <SiteFrame tournamentCode={tournamentCode}>
        <EmptyState title="대회를 찾을 수 없습니다" description="요청한 대회 코드와 일치하는 공개 대회가 없습니다." />
      </SiteFrame>
    );
  }

  return (
    <SiteFrame tournamentCode={tournamentCode}>
      <TournamentHome dataset={dataset} />
    </SiteFrame>
  );
}
