import Link from "next/link";

export default function NotFound() {
  return (
    <main id="main-content" className="page" tabIndex={-1}>
      <section className="hero" aria-labelledby="not-found-title">
        <p className="eyebrow">404</p>
        <h1 id="not-found-title">페이지를 찾을 수 없습니다</h1>
        <p className="lead">주소를 다시 확인하거나 대회 홈으로 이동하세요.</p>
        <Link href="/tournaments/seoul-open-2026">대회 홈으로 이동</Link>
      </section>
    </main>
  );
}
