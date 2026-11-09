"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { Suspense } from "react";
import { api, type Decision, type Employee, type Ticket } from "@/lib/api";
import { useApi } from "@/components/useApi";
import { Badge, Card, Empty, ErrorNote, Loading, Mono } from "@/components/ui";
import { DecisionPanel } from "@/components/DecisionPanel";
import { StatusBadge, slaCountdown } from "@/components/domain";

/**
 * El contenido va dentro de un Suspense porque lee la ruta con un hook dinámico. Next no
 * puede prerenderizar eso, y envolverlo es además lo que da un estado de carga de verdad.
 */
export default function TicketDetailPage() {
  return (
    <Suspense fallback={<Loading />}>
      <TicketDetail />
    </Suspense>
  );
}

function TicketDetail() {
  const id = String(useParams().id ?? "");
  const { data, error, loading } = useApi<[Ticket, Decision[], Employee[]]>(
    () => Promise.all([api.ticket(id), api.decisions(id), api.employees()]),
    [id],
  );

  if (loading) return <Loading />;
  if (error) return <ErrorNote message={error} />;
  if (!data) return null;

  const [ticket, decisions, employees] = data;
  const sla = slaCountdown(ticket.slaDeadline);
  const latest = decisions[0];

  return (
    <div className="space-y-6">
      <Link
        href="/tickets"
        className="inline-block text-sm text-[var(--text-muted)] hover:text-[var(--text)] transition-colors"
      >
        ← Tickets
      </Link>

      <header className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-xl font-semibold tracking-tight">{ticket.title}</h1>
          <StatusBadge status={ticket.status} />
        </div>
        <p className="mt-3 text-sm leading-relaxed text-[var(--text-muted)]">
          {ticket.description}
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-3 text-xs">
          <Badge tone={ticket.impact === "CRITICAL" ? "danger" : "neutral"}>
            impacto {ticket.impact}
          </Badge>
          <span
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
          <Mono>{ticket.id.slice(0, 8)}</Mono>
        </div>
      </header>

      {latest ? (
        <DecisionPanel decision={latest} employees={employees} />
      ) : (
        <Empty>
          Todavía no se decidió nada sobre este ticket. El worker lo toma en la próxima pasada.
        </Empty>
      )}

      {decisions.length > 1 && (
        <Card title="Decisiones anteriores" subtitle={`${decisions.length - 1} más`}>
          <ul className="space-y-2 text-sm">
            {decisions.slice(1).map((decision) => (
              <li key={decision.id} className="flex items-center justify-between gap-3">
                <span className="text-[var(--text-muted)] truncate">{decision.rationale}</span>
                <Mono>{decision.policyVersion}</Mono>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
