import { expect, request, test, type APIRequestContext } from "@playwright/test";

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? "http://localhost:8080";
const adminAuth = {
  username: process.env.E2E_ADMIN_USER ?? "admin",
  password: process.env.E2E_ADMIN_PASSWORD ?? "admin1234",
};

type E2EState = {
  tournamentCode: string;
  tournamentName: string;
  player1Name: string;
  player2Name: string;
};

test.describe.configure({ mode: "serial" });

let state: E2EState;

test.beforeAll(async () => {
  const api = await request.newContext({
    baseURL: apiBaseUrl,
    httpCredentials: adminAuth,
    extraHTTPHeaders: { Accept: "application/json" },
  });

  state = await seedTournament(api);
  await api.dispose();
});

test("public tournament pages render seeded backend data", async ({ page }) => {
  await page.goto(`/tournaments/${state.tournamentCode}`);
  await expect(page.getByRole("heading", { name: state.tournamentName })).toBeVisible();

  await page.goto(`/tournaments/${state.tournamentCode}/players`);
  await expect(page.getByText(state.player1Name)).toBeVisible();
  await expect(page.getByText(state.player2Name)).toBeVisible();

  await page.goto(`/tournaments/${state.tournamentCode}/matches`);
  await expect(page.getByText(state.player1Name).first()).toBeVisible();
  await expect(page.getByText(state.player2Name).first()).toBeVisible();
});

test("admin login stores basic auth session and opens dashboard", async ({ page }) => {
  await page.goto("/admin/login");
  await page.locator("select").selectOption("ADMIN");
  await page.locator('input[autocomplete="username"]').fill(adminAuth.username);
  await page.locator('input[autocomplete="current-password"]').fill(adminAuth.password);
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/admin$/);
  await expect
    .poll(() =>
      page.evaluate(() => window.sessionStorage.getItem("showdown.basicAuth")),
    )
    .toContain('"role":"ADMIN"');
});

async function seedTournament(api: APIRequestContext): Promise<E2EState> {
  const suffix = Date.now().toString(36);
  const tournamentCode = `e2e-${suffix}`;
  const tournamentName = `E2E Showdown Open ${suffix}`;
  const player1Name = `Alice E2E ${suffix}`;
  const player2Name = `Bob E2E ${suffix}`;
  const courtName = `Court E2E ${suffix}`;

  const tournament = await postJson(api, "/api/admin/tournaments", {
    code: tournamentCode,
    name: tournamentName,
    location: "Seoul",
    startDate: "2026-07-01",
    endDate: "2026-07-02",
    timezone: "Asia/Seoul",
    status: "PUBLISHED",
    defaultLanguage: "ko",
  });

  const division = await postJson(api, `/api/admin/tournaments/${tournament.id}/divisions`, {
    name: "Open Singles",
    code: `OPEN-${suffix}`,
    category: "OPEN",
    sortOrder: 1,
    active: true,
  });

  const player1 = await postJson(api, `/api/admin/tournaments/${tournament.id}/players`, {
    displayName: player1Name,
    countryCode: "KOR",
    divisionId: division.id,
    seedNo: 1,
    entryNo: 1,
    clubName: "Blue Club",
    status: "ACTIVE",
  });
  const player2 = await postJson(api, `/api/admin/tournaments/${tournament.id}/players`, {
    displayName: player2Name,
    countryCode: "USA",
    divisionId: division.id,
    seedNo: 2,
    entryNo: 2,
    clubName: "Red Club",
    status: "ACTIVE",
  });

  const stage = await postJson(api, `/api/admin/tournaments/${tournament.id}/stages`, {
    divisionId: division.id,
    name: "Preliminary",
    stageType: "ROUND_ROBIN",
    sortOrder: 1,
  });

  const group = await postJson(api, `/api/admin/tournaments/${tournament.id}/groups`, {
    divisionId: division.id,
    stageId: stage.id,
    code: `A-${suffix}`,
    name: "Group A",
    groupType: "LEAGUE",
    sortOrder: 1,
  });

  await postJson(api, `/api/admin/tournaments/${tournament.id}/matches`, {
    divisionId: division.id,
    stageId: stage.id,
    groupId: group.id,
    matchNo: 1,
    scheduledAt: "2026-07-01T10:00:00+09:00",
    courtName,
    refereeName: "Ref E2E",
    player1TournamentPlayerId: player1.id,
    player2TournamentPlayerId: player2.id,
    status: "SCHEDULED",
  });

  return { tournamentCode, tournamentName, player1Name, player2Name };
}

async function postJson(api: APIRequestContext, path: string, body: object) {
  const response = await api.post(path, {
    data: body,
    headers: { "Content-Type": "application/json" },
  });
  expect(response.ok(), `${path} returned ${response.status()}: ${await response.text()}`).toBeTruthy();
  return response.json();
}
