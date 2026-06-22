import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Showdown Tournament Manager",
  description: "접근성을 고려한 쇼다운 대회 공개 조회 및 점수 입력 프론트엔드",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <a className="skip-link" href="#main-content">
          본문으로 바로가기
        </a>
        {children}
      </body>
    </html>
  );
}
