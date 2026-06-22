import Link from "next/link";
import type { ReactNode } from "react";

const adminItems = [
  { href: "/admin", label: "대시보드" },
  { href: "/admin/tournament", label: "대회 설정" },
  { href: "/admin/players", label: "선수 관리" },
  { href: "/admin/officials", label: "심판 관리" },
  { href: "/admin/stages", label: "단계 관리" },
  { href: "/admin/groups", label: "조 편성" },
  { href: "/admin/matches", label: "일정 관리" },
  { href: "/admin/scoring", label: "점수 입력" },
];

export function AdminFrame({
  tournamentCode,
  currentPath,
  children,
}: {
  tournamentCode: string;
  currentPath: string;
  children: ReactNode;
}) {
  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" href="/admin">
            Showdown Admin
          </Link>
          <nav aria-label="관리자 메뉴">
            <ul className="nav-list">
              {adminItems.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} aria-current={currentPath === item.href ? "page" : undefined}>
                    {item.label}
                  </Link>
                </li>
              ))}
              <li>
                <Link href={`/tournaments/${tournamentCode}`}>공개 화면</Link>
              </li>
            </ul>
          </nav>
        </div>
      </header>
      <main id="main-content" className="page" tabIndex={-1}>
        {children}
      </main>
    </div>
  );
}
