import type { ReactNode } from "react";

export function Card({
  title,
  subtitle,
  accent,
  children,
}: {
  title?: string;
  subtitle?: string;
  accent?: string;
  children: ReactNode;
}) {
  return (
    <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] overflow-hidden">
      {title && (
        <header
          className="px-5 py-3.5 border-b border-[var(--border)] flex items-baseline gap-3"
          style={accent ? { boxShadow: `inset 3px 0 0 ${accent}` } : undefined}
        >
          <h2 className="text-sm font-semibold tracking-wide uppercase text-[var(--text)]">
            {title}
          </h2>
          {subtitle && <p className="text-xs text-[var(--text-muted)]">{subtitle}</p>}
        </header>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}

/** Barra de probabilidad. El número siempre va al lado: una barra sola no se puede leer. */
export function Bar({
  label,
  value,
  color = "var(--model)",
  hint,
}: {
  label: string;
  value: number;
  color?: string;
  hint?: string;
}) {
  const pct = Math.round(value * 100);
  return (
    <div className="grid grid-cols-[minmax(0,13rem)_1fr_3rem] items-center gap-3 py-1.5">
      <span className="text-sm text-[var(--text-muted)] leading-snug" title={hint ?? label}>
        {label}
      </span>
      <div className="h-2 rounded-full bg-[var(--surface-raised)] overflow-hidden">
        <div
          className="bar-fill h-full rounded-full"
          style={{ width: `${Math.max(pct, 1)}%`, background: color }}
        />
      </div>
      <span className="text-sm font-mono tabular-nums text-right text-[var(--text)]">{pct}%</span>
    </div>
  );
}

const TONE: Record<string, { bg: string; fg: string }> = {
  ok: { bg: "rgba(52,211,153,0.14)", fg: "var(--ok)" },
  warn: { bg: "rgba(251,191,36,0.14)", fg: "var(--warn)" },
  danger: { bg: "rgba(248,113,113,0.14)", fg: "var(--danger)" },
  model: { bg: "rgba(167,139,250,0.14)", fg: "var(--model)" },
  neutral: { bg: "rgba(139,154,179,0.12)", fg: "var(--text-muted)" },
};

export function Badge({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: keyof typeof TONE;
}) {
  const { bg, fg } = TONE[tone] ?? TONE.neutral;
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap"
      style={{ background: bg, color: fg }}
    >
      {children}
    </span>
  );
}

export function Stat({
  label,
  value,
  hint,
  tone = "neutral",
}: {
  label: string;
  value: string;
  hint?: string;
  tone?: keyof typeof TONE;
}) {
  const { fg } = TONE[tone] ?? TONE.neutral;
  return (
    <div className="rounded-xl border border-[var(--border)] bg-[var(--surface)] px-5 py-4">
      <p className="text-xs uppercase tracking-wide text-[var(--text-muted)]">{label}</p>
      <p className="mt-1.5 text-3xl font-semibold tabular-nums" style={{ color: fg }}>
        {value}
      </p>
      {hint && <p className="mt-1 text-xs text-[var(--text-muted)]">{hint}</p>}
    </div>
  );
}

export function Mono({ children }: { children: ReactNode }) {
  return (
    <code className="font-mono text-xs text-[var(--text-muted)] bg-[var(--surface-raised)] rounded px-1.5 py-0.5">
      {children}
    </code>
  );
}

export function Loading() {
  return <p className="text-sm text-[var(--text-muted)]">Cargando…</p>;
}

export function ErrorNote({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="rounded-xl border px-5 py-4 text-sm"
      style={{ borderColor: "rgba(248,113,113,0.35)", color: "var(--danger)" }}
    >
      {message}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-[var(--border)] px-5 py-10 text-center text-sm text-[var(--text-muted)]">
      {children}
    </div>
  );
}
