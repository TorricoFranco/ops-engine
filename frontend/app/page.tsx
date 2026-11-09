"use client";

import Link from "next/link";
import { api, type Metrics, type Ticket } from "@/lib/api";
import { useApi } from "@/components/useApi";
import { Card, Empty, ErrorNote, Loading, Stat } from "@/components/ui";
import { StatusBadge, slaCountdown } from "@/components/domain";

// La API omite los campos nulos, así que acá llegan como undefined. El `== null` cubre los
// dos casos; con `=== null` el porcentaje salía NaN.
const pct = (value: number | null | undefined) =>
  value == null ? "—" : `${Math.round(value * 100)}%`;

const plural = (count: number, one: string, many: string) =>
  `${count} ${count === 1 ? one : many}`;

export default function DashboardPage() {
  const { data, error, loading } = useApi<[Metrics, Ticket[]]>(() =>
    Promise.all([api.metrics(), api.tickets()]),
  );

  if (loading) return <Loading />;
  if (error) return <ErrorNote message={error} />;
  if (!data) return null;

  const [metrics, tickets] = data;
  const waiting = tickets
    .filter((ticket) => ticket.status === "REVISION_REQUERIDA" || ticket.status === "NUEVO")
    .sort((a, b) => a.slaDeadline.localeCompare(b.slaDeadline))
    .slice(0, 6);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Panel de operaciones</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          El modelo estima, el código decide y cada decisión queda explicada.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Decisiones" value={String(metrics.decisions)} />
        <Stat
          label="Asignadas solas"
          value={String(metrics.assigned)}
          tone="ok"
          hint="Sin intervención de nadie"
        />
        <Stat
          label="Tasa de revisión"
          value={pct(metrics.reviewRate)}
          tone="warn"
          hint={plural(metrics.sentToReview, "esperó a una persona", "esperaron a una persona")}
        />
        <Stat
          label="Tasa de override"
          value={pct(metrics.overrideRate)}
          tone={metrics.overrideRate == null ? "neutral" : "model"}
          hint={
            metrics.comparable === 0
              ? "Sin casos comparables todavía"
              : `${metrics.overrides} de ${metrics.comparable} resoluciones`
          }
        />
      </div>

      <Card title="Lo que está esperando" subtitle="lo más urgente primero">
        {waiting.length === 0 ? (
          <Empty>No hay nada esperando. Todo lo que entró se resolvió solo.</Empty>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {waiting.map((ticket) => {
              const sla = slaCountdown(ticket.slaDeadline);
              return (
                <li key={ticket.id}>
                  <Link
                    href={`/tickets/${ticket.id}`}
                    className="flex items-center justify-between gap-4 py-3 group"
                  >
                    <span className="min-w-0">
                      <span className="block text-sm text-[var(--text)] truncate group-hover:text-[var(--accent)] transition-colors">
                        {ticket.title}
                      </span>
                      <span
                        className="block text-xs mt-0.5"
                        style={{
                          color:
                            sla.tone === "danger"
                              ? "var(--danger)"
                              : sla.tone === "warn"
                                ? "var(--warn)"
                                : "var(--text-muted)",
                        }}
                      >
                        SLA {sla.text}
                      </span>
                    </span>
                    <StatusBadge status={ticket.status} />
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <Card title="Cómo funciona">
        <ol className="space-y-3 text-sm leading-relaxed text-[var(--text-muted)]">
          <li>
            <span className="text-[var(--model)] font-medium">1. El modelo estima.</span>{" "}
            Responde preguntas acotadas sobre el texto: qué competencia hace falta, cuánto le
            impide operar al cliente, si menciona una caída. Nunca decide a quién asignarlo.
          </li>
          <li>
            <span className="text-[var(--accent)] font-medium">2. El código decide.</span>{" "}
            Cruza esas estimaciones con lo que el sistema sí sabe: disponibilidad, carga,
            habilitación y el vencimiento real del SLA. Un SLA por vencer pesa más que la
            urgencia que transmita el texto.
          </li>
          <li>
            <span className="text-[var(--warn)] font-medium">3. Ante la duda, una persona.</span>{" "}
            Si el proveedor falla, si ninguna competencia encaja o si nadie cumple los
            requisitos, el ticket espera a un supervisor en lugar de asignarse mal.
          </li>
        </ol>
      </Card>
    </div>
  );
}
