"use client";

import Link from "next/link";
import { useState } from "react";
import { saveAuthSession } from "@/lib/auth";
import type { BasicAuthSession } from "@/lib/api";

export function AdminLogin() {
  const [draft, setDraft] = useState<BasicAuthSession>({
    username: "",
    password: "",
    role: "ADMIN",
  });
  const [message, setMessage] = useState("계정 정보를 입력하면 인증 정보가 현재 브라우저 세션에만 저장됩니다.");

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    saveAuthSession(draft);
    setMessage(`${draft.role} 역할로 로그인 정보를 저장했습니다. 이동합니다.`);

    const nextPath = draft.role === "REFEREE" ? "/admin/scoring" : draft.role === "PLAYER" ? "/players" : "/admin";
    window.setTimeout(() => {
      window.location.href = nextPath;
    }, 150);
  }

  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" href="/">
            Showdown Tournament Manager
          </Link>
          <nav aria-label="주요 메뉴">
            <ul className="nav-list">
              <li>
                <Link href="/admin">관리자</Link>
              </li>
              <li>
                <Link href="/admin/scoring">점수 입력</Link>
              </li>
            </ul>
          </nav>
        </div>
      </header>

      <main id="main-content" className="page" tabIndex={-1}>
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Basic Auth</p>
          <h1 id="page-title">관리자 로그인</h1>
          <p className="lead">
            비밀번호는 코드나 환경변수에 저장하지 않고 브라우저 sessionStorage에만 보관합니다.
          </p>
        </section>

        <section className="panel" aria-labelledby="login-form-title">
          <h2 id="login-form-title">계정 입력</h2>
          <div aria-live="polite" aria-atomic="true" className="alert" role="status">
            {message}
          </div>
          <form className="form-grid" onSubmit={onSubmit}>
            <label className="field">
              <span>역할</span>
              <select
                value={draft.role}
                onChange={(event) => setDraft({ ...draft, role: event.target.value as BasicAuthSession["role"] })}
              >
                <option value="ADMIN">관리자</option>
                <option value="REFEREE">심판/기록원</option>
                <option value="PLAYER">선수</option>
              </select>
            </label>
            <label className="field">
              <span>아이디</span>
              <input
                autoComplete="username"
                value={draft.username}
                onChange={(event) => setDraft({ ...draft, username: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>비밀번호</span>
              <input
                autoComplete="current-password"
                type="password"
                value={draft.password}
                onChange={(event) => setDraft({ ...draft, password: event.target.value })}
                required
              />
            </label>
            <button type="submit">로그인 정보 저장</button>
          </form>
        </section>
      </main>
    </div>
  );
}
