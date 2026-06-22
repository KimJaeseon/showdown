package com.showdown.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShowdownScenarioReportTests {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestedTournamentScenarioIsCreatedAndReported() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        JsonNode tournament = postAdmin("/api/admin/tournaments", Map.of(
                "code", "scenario-0627-" + suffix,
                "name", "6월 27-28일 테스트 대회",
                "location", "ㅅ 경기장",
                "startDate", "2026-06-27",
                "endDate", "2026-06-28",
                "timezone", "Asia/Seoul",
                "status", "PUBLISHED",
                "defaultLanguage", "ko"
        ));
        String tournamentId = tournament.get("id").asText();

        JsonNode maleDivision = postAdmin("/api/admin/tournaments/" + tournamentId + "/divisions", Map.of(
                "name", "남자부",
                "code", "MEN",
                "category", "MALE",
                "sortOrder", 1,
                "active", true
        ));
        JsonNode femaleDivision = postAdmin("/api/admin/tournaments/" + tournamentId + "/divisions", Map.of(
                "name", "여자부",
                "code", "WOMEN",
                "category", "FEMALE",
                "sortOrder", 2,
                "active", true
        ));

        List<String> malePlayers = createPlayers(tournamentId, maleDivision.get("id").asText(), "남자선수", 40);
        List<String> femalePlayers = createPlayers(tournamentId, femaleDivision.get("id").asText(), "여자선수", 23);
        List<String> officialIds = createOfficials(tournamentId, 12);

        JsonNode maleLeagueStage = createStage(tournamentId, maleDivision.get("id").asText(), "남자 조별리그", "ROUND_ROBIN", 1);
        JsonNode maleKnockoutStage = createStage(tournamentId, maleDivision.get("id").asText(), "남자 토너먼트", "KNOCKOUT", 2);
        JsonNode femaleLeagueStage = createStage(tournamentId, femaleDivision.get("id").asText(), "여자 조별리그", "ROUND_ROBIN", 1);
        JsonNode femaleKnockoutStage = createStage(tournamentId, femaleDivision.get("id").asText(), "여자 토너먼트", "KNOCKOUT", 2);

        List<List<String>> maleGroups = splitPlayers(malePlayers, 9);
        List<List<String>> femaleGroups = splitPlayers(femalePlayers, 4);
        List<String> maleGroupIds = createGroups(tournamentId, maleDivision.get("id").asText(), maleLeagueStage.get("id").asText(), "M", maleGroups.size());
        List<String> femaleGroupIds = createGroups(tournamentId, femaleDivision.get("id").asText(), femaleLeagueStage.get("id").asText(), "W", femaleGroups.size());

        List<MatchPlan> plans = new ArrayList<>();
        plans.addAll(planRoundRobin(maleDivision.get("id").asText(), maleLeagueStage.get("id").asText(), maleGroupIds, maleGroups, List.of("1", "2", "3", "4"), 1));
        plans.addAll(planRoundRobin(femaleDivision.get("id").asText(), femaleLeagueStage.get("id").asText(), femaleGroupIds, femaleGroups, List.of("5", "6"), 1001));
        plans.addAll(planKnockout(maleDivision.get("id").asText(), maleKnockoutStage.get("id").asText(), malePlayers.subList(0, 16), List.of("1", "2", "3", "4"), 2001, "남자 16강"));
        plans.addAll(planKnockout(femaleDivision.get("id").asText(), femaleKnockoutStage.get("id").asText(), femalePlayers.subList(0, 8), List.of("5", "6"), 3001, "여자 8강"));

        List<JsonNode> createdMatches = new ArrayList<>();
        for (MatchPlan plan : plans) {
            createdMatches.add(createMatch(tournamentId, plan, officialIds));
        }

        JsonNode persistedMatches = getAdmin("/api/admin/tournaments/" + tournamentId + "/matches");
        ScenarioSummary summary = summarize(createdMatches, persistedMatches);
        writeReports(tournamentId, summary, maleGroups, femaleGroups);
    }

    private List<String> createPlayers(String tournamentId, String divisionId, String prefix, int count) throws Exception {
        List<String> playerIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            JsonNode player = postAdmin("/api/admin/tournaments/" + tournamentId + "/players", Map.of(
                    "displayName", prefix + String.format("%02d", i),
                    "countryCode", "KOR",
                    "divisionId", divisionId,
                    "seedNo", i,
                    "entryNo", i,
                    "clubName", "테스트클럽",
                    "status", "ACTIVE"
            ));
            playerIds.add(player.get("id").asText());
        }
        return playerIds;
    }

    private List<String> createOfficials(String tournamentId, int count) throws Exception {
        List<String> officialIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            JsonNode official = postAdmin("/api/admin/tournaments/" + tournamentId + "/officials", Map.of(
                    "name", "심판" + String.format("%02d", i),
                    "shortCode", "R" + String.format("%02d", i),
                    "roleName", "REFEREE",
                    "active", true
            ));
            officialIds.add(official.get("id").asText());
        }
        return officialIds;
    }

    private JsonNode createStage(String tournamentId, String divisionId, String name, String stageType, int sortOrder) throws Exception {
        return postAdmin("/api/admin/tournaments/" + tournamentId + "/stages", Map.of(
                "divisionId", divisionId,
                "name", name,
                "stageType", stageType,
                "sortOrder", sortOrder
        ));
    }

    private List<String> createGroups(String tournamentId, String divisionId, String stageId, String prefix, int count) throws Exception {
        List<String> groupIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            char code = (char) ('A' + i);
            JsonNode group = postAdmin("/api/admin/tournaments/" + tournamentId + "/groups", Map.of(
                    "divisionId", divisionId,
                    "stageId", stageId,
                    "code", prefix + "-" + code,
                    "name", prefix + "조 " + code,
                    "groupType", "LEAGUE",
                    "sortOrder", i + 1
            ));
            groupIds.add(group.get("id").asText());
        }
        return groupIds;
    }

    private List<List<String>> splitPlayers(List<String> players, int groupCount) {
        List<List<String>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(new ArrayList<>());
        }
        for (int i = 0; i < players.size(); i++) {
            groups.get(i % groupCount).add(players.get(i));
        }
        return groups;
    }

    private List<MatchPlan> planRoundRobin(String divisionId, String stageId, List<String> groupIds, List<List<String>> groups, List<String> courtNumbers, int matchNoStart) {
        List<MatchPlan> plans = new ArrayList<>();
        OffsetDateTime start = LocalDateTime.of(2026, 6, 27, 9, 30).atOffset(KST);
        int matchNo = matchNoStart;
        int sequence = 0;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            List<String> groupPlayers = groups.get(groupIndex);
            for (int i = 0; i < groupPlayers.size(); i++) {
                for (int j = i + 1; j < groupPlayers.size(); j++) {
                    String court = courtNumbers.get(sequence % courtNumbers.size());
                    OffsetDateTime scheduledAt = start.plusMinutes(15L * (sequence / courtNumbers.size()));
                    plans.add(new MatchPlan(divisionId, stageId, groupIds.get(groupIndex), matchNo++, scheduledAt, court, groupPlayers.get(i), groupPlayers.get(j), "조별리그"));
                    sequence++;
                }
            }
        }
        return plans;
    }

    private List<MatchPlan> planKnockout(String divisionId, String stageId, List<String> seededPlayers, List<String> courtNumbers, int matchNoStart, String firstRoundName) {
        List<MatchPlan> plans = new ArrayList<>();
        OffsetDateTime start = LocalDateTime.of(2026, 6, 28, 9, 30).atOffset(KST);
        int matchNo = matchNoStart;
        int sequence = 0;
        List<String> currentRoundPlayers = new ArrayList<>(seededPlayers);
        String roundName = firstRoundName;
        while (currentRoundPlayers.size() >= 2) {
            List<String> nextRoundPlaceholders = new ArrayList<>();
            for (int i = 0; i < currentRoundPlayers.size(); i += 2) {
                String court = courtNumbers.get(sequence % courtNumbers.size());
                OffsetDateTime scheduledAt = start.plusMinutes(30L * (sequence / courtNumbers.size()));
                plans.add(new MatchPlan(divisionId, stageId, null, matchNo++, scheduledAt, court, currentRoundPlayers.get(i), currentRoundPlayers.get(i + 1), roundName));
                nextRoundPlaceholders.add(currentRoundPlayers.get(i));
                sequence++;
            }
            currentRoundPlayers = nextRoundPlaceholders;
            roundName = "다음 라운드";
        }
        return plans;
    }

    private JsonNode createMatch(String tournamentId, MatchPlan plan, List<String> officialIds) throws Exception {
        Map<String, Object> request = new HashMap<>();
        int officialOffset = Math.max(0, Integer.parseInt(plan.courtName()) - 1) * 2;
        request.put("divisionId", plan.divisionId());
        request.put("stageId", plan.stageId());
        request.put("groupId", plan.groupId());
        request.put("matchNo", plan.matchNo());
        request.put("scheduledAt", plan.scheduledAt().toString());
        request.put("courtName", plan.courtName());
        request.put("durationMinutes", 15);
        request.put("refereeOfficialIds", List.of(
                officialIds.get(officialOffset),
                officialIds.get((officialOffset + 1) % officialIds.size())
        ));
        request.put("player1TournamentPlayerId", plan.player1Id());
        request.put("player2TournamentPlayerId", plan.player2Id());
        request.put("status", "SCHEDULED");
        return postAdmin("/api/admin/tournaments/" + tournamentId + "/matches", request);
    }

    private ScenarioSummary summarize(List<JsonNode> createdMatches, JsonNode persistedMatches) {
        long courtPersistedCount = 0;
        long refereePersistedCount = 0;
        long maleCourtViolationCount = 0;
        long femaleCourtViolationCount = 0;
        OffsetDateTime latestLeagueMatchTime = null;

        for (JsonNode match : createdMatches) {
            if (!match.get("courtName").isNull()) {
                courtPersistedCount++;
            }
            if (match.has("refereeNames") && match.get("refereeNames").size() == 2) {
                refereePersistedCount++;
            }
            String player1Name = match.get("player1Name").asText();
            String courtName = match.get("courtName").isNull() ? "" : match.get("courtName").asText();
            if (player1Name.startsWith("남자") && !List.of("1", "2", "3", "4").contains(courtName)) {
                maleCourtViolationCount++;
            }
            if (player1Name.startsWith("여자") && !List.of("5", "6").contains(courtName)) {
                femaleCourtViolationCount++;
            }
            if (!match.get("groupId").isNull()) {
                OffsetDateTime scheduledAt = OffsetDateTime.parse(match.get("scheduledAt").asText());
                if (latestLeagueMatchTime == null || scheduledAt.isAfter(latestLeagueMatchTime)) {
                    latestLeagueMatchTime = scheduledAt;
                }
            }
        }

        return new ScenarioSummary(
                createdMatches.size(),
                persistedMatches.size(),
                courtPersistedCount,
                refereePersistedCount,
                maleCourtViolationCount,
                femaleCourtViolationCount,
                latestLeagueMatchTime
        );
    }

    private void writeReports(String tournamentId, ScenarioSummary summary, List<List<String>> maleGroups, List<List<String>> femaleGroups) throws Exception {
        Path resultPath = Path.of("..", "테스트시나리오_실행결과.txt").normalize();
        Path changePath = Path.of("..", "테스트시나리오_기능변경필요사항.txt").normalize();

        String result = """
                테스트 시나리오 실행 결과
                ==========================
                실행 방식: Spring Boot MockMvc 통합 테스트
                생성 대회 ID: %s
                대회 기간: 2026-06-27 ~ 2026-06-28
                장소/코트 수: ㅅ 경기장 / 6개 코트
                선수: 남자 40명, 여자 23명
                심판: 12명 등록
                조 구성: 남자 9개조, 여자 4개조
                남자 조별 인원 분포: %s
                여자 조별 인원 분포: %s
                조별리그 시간: 2026-06-27 09:30부터 시작, 마지막 배정 %s KST
                토너먼트: 남자 16강 시작, 여자 8강 시작
                경기 생성 수: %d건
                관리자 경기 조회 수: %d건

                검증 결과
                - 대회/부문/선수/심판/스테이지/조/경기 생성 API는 정상 응답했습니다.
                - 조별리그는 요청 시간 범위인 2026-06-27 09:30 ~ 18:00 안에 배정 가능한 데이터로 생성했습니다.
                - 현재 응답 기준 courtName 저장 건수: %d건
                - 현재 응답 기준 refereeName 저장 건수: %d건
                - 남자 1~4코트 배정 검증 불가/위반 수: %d건
                - 여자 5~6코트 배정 검증 불가/위반 수: %d건

                판정
                - 기본 등록 흐름은 통과했습니다.
                - 경기장/심판 배정 정보는 현재 백엔드에서 저장/조회되지 않아 요구사항을 완전히 검증할 수 없습니다.
                """.formatted(
                tournamentId,
                groupSizes(maleGroups),
                groupSizes(femaleGroups),
                summary.latestLeagueMatchTime().withOffsetSameInstant(KST).toLocalDateTime(),
                summary.createdMatchCount(),
                summary.persistedMatchCount(),
                summary.courtPersistedCount(),
                summary.refereePersistedCount(),
                summary.maleCourtViolationCount(),
                summary.femaleCourtViolationCount()
        );

        String change = """
                테스트 시나리오 기준 기능 변경 필요사항
                ======================================
                1. 경기장 정보 저장 필요
                - MatchRequest와 MatchResponse에는 courtName이 있으나 Match 엔티티와 TournamentAdminService.applyMatch, ApiMapper에서 실제 저장/조회가 되지 않습니다.
                - matches 테이블에 court_name 컬럼을 추가하고 Match 엔티티 getter/setter 및 매핑 로직을 연결해야 합니다.

                2. 경기별 심판 2명 배정 모델 필요
                - 현재 MatchRequest/Response는 refereeName 단일 문자열만 표현하며, 이 값도 저장되지 않습니다.
                - 요청 조건은 경기마다 심판 2명 배정이므로 MatchOfficial 같은 연결 테이블로 경기-심판 다대다 배정을 표현하는 것이 적합합니다.
                - 최소한 심판 2명 미만 등록 시 검증 실패, 동일 시간대 심판 중복 배정 방지 로직이 필요합니다.

                3. 코트별 동시간대 중복 검증 필요
                - 현재 API는 scheduledAt만 저장하고 경기 소요 시간/코트 점유 시간을 관리하지 않습니다.
                - courtName과 예상 경기 시간 또는 종료 시간을 함께 관리해야 1~4코트 남자, 5~6코트 여자 배정 충돌을 검증할 수 있습니다.

                4. 자동 조 편성/라운드로빈/토너먼트 스케줄 생성 기능 필요
                - 현재는 조, 경기, 토너먼트 경기를 모두 수동 등록해야 합니다.
                - 남자 40명 9개조, 여자 23명 4개조처럼 인원과 조 수를 입력하면 조 편성 및 모든 조별리그 대진을 생성하는 서비스가 필요합니다.
                - 남자 16강, 여자 8강 시작 조건을 기반으로 토너먼트 브래킷을 생성하고 이후 라운드 참가자를 승자 슬롯으로 연결하는 모델이 필요합니다.

                5. 리포트/검증 API 또는 테스트 유틸 보강 필요
                - 운영 화면에서 요구사항 충족 여부를 확인하려면 총 경기 수, 코트별 경기 수, 시간대별 충돌, 심판 배정 누락을 요약하는 조회 기능이 있으면 좋습니다.
                """;

        Files.writeString(resultPath, result, StandardCharsets.UTF_8);
        Files.writeString(changePath, change, StandardCharsets.UTF_8);
    }

    private String groupSizes(List<List<String>> groups) {
        return groups.stream().map(group -> Integer.toString(group.size())).toList().toString();
    }

    private JsonNode postAdmin(String path, Object body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getAdmin(String path) throws Exception {
        String response = mockMvc.perform(get(path)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private record MatchPlan(
            String divisionId,
            String stageId,
            String groupId,
            int matchNo,
            OffsetDateTime scheduledAt,
            String courtName,
            String player1Id,
            String player2Id,
            String roundName
    ) {
    }

    private record ScenarioSummary(
            int createdMatchCount,
            int persistedMatchCount,
            long courtPersistedCount,
            long refereePersistedCount,
            long maleCourtViolationCount,
            long femaleCourtViolationCount,
            OffsetDateTime latestLeagueMatchTime
    ) {
    }
}
