import Link from "next/link";
import type { ReactNode } from "react";

const navItems = [
  { href: "", label: "대회 홈" },
  { href: "/players", label: "선수 목록" },
  { href: "/matches", label: "경기 일정" },
  { href: "/groups", label: "조별 순위" },
  { href: "/rankings", label: "최종 순위" },
];

export function SiteFrame({
  tournamentCode,
  children,
}: {
  tournamentCode: string;
  children: ReactNode;
}) {
  const baseHref = `/tournaments/${tournamentCode}`;

  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" href={baseHref}>
            Showdown Tournament Manager
          </Link>
          <nav aria-label="공개 조회 메뉴">
            <ul className="nav-list">
              {navItems.map((item) => (
                <li key={item.href || "home"}>
                  <Link href={`${baseHref}${item.href}`}>{item.label}</Link>
                </li>
              ))}
              <li>
                <Link href="/admin/scoring">점수 입력</Link>
              </li>
            </ul>
          </nav>
        </div>
      </header>
      <main id="main-content" className="page" tabIndex={-1}>
        {children}
      </main>
      <footer className="footer">
        <p>모든 핵심 정보는 키보드와 스크린리더로 접근할 수 있도록 구성되었습니다.</p>
      </footer>
    </div>
  );
}
