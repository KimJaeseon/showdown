import Link from "next/link";

export function EmptyState({
  title,
  description,
  actionHref,
  actionLabel,
}: {
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
}) {
  return (
    <section className="panel" aria-labelledby="empty-state-title">
      <h2 id="empty-state-title">{title}</h2>
      <p>{description}</p>
      {actionHref && actionLabel ? <Link href={actionHref}>{actionLabel}</Link> : null}
    </section>
  );
}

export function ErrorState({ title, message }: { title: string; message: string }) {
  return (
    <section className="alert error" role="alert" aria-labelledby="error-state-title">
      <h2 id="error-state-title">{title}</h2>
      <p>{message}</p>
    </section>
  );
}
