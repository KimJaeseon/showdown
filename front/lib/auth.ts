import type { BasicAuthSession } from "./api";

const AUTH_KEY = "showdown.basicAuth";

export function readAuthSession(): BasicAuthSession | null {
  if (typeof window === "undefined") return null;
  const raw = window.sessionStorage.getItem(AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as BasicAuthSession;
  } catch {
    window.sessionStorage.removeItem(AUTH_KEY);
    return null;
  }
}

export function saveAuthSession(session: BasicAuthSession) {
  window.sessionStorage.setItem(AUTH_KEY, JSON.stringify(session));
}

export function clearAuthSession() {
  window.sessionStorage.removeItem(AUTH_KEY);
}
