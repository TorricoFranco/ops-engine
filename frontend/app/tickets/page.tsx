"use client";

import Link from "next/link";
import { api, type Ticket } from "@/lib/api";
import { useApi } from "@/components/useApi";
import { Badge, Empty, ErrorNote, Loading } from "@/components/ui";
import { StatusBadge, slaCountdown } from "@/components/domain";

const IMPACT_TONE = {
  LOW: "neutral",
  MEDIUM: "neutral",
  HIGH: "warn",
  CRITICAL: "danger",
} as const;

export default function TicketsPage() {
  const { data: tickets, error, loading } = useApi<Ticket[]>(() => api.tickets());

  if (loading) return <Loading />;
  if (error) return <ErrorNote message={error} />;
  if (!tickets) return null;

  const sorted = [...tickets].sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tickets</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          {tickets.length} en total. Abrí uno para ver cómo se decidió.
        </p>
      </div>

      {sorted.length === 0 ? (
        <Empty>
          No hay tickets. Levantá la API con el perfil <code className="font-mono">dev</code>{" "}
          para cargar los de ejemplo.
        </Empty>
      ) : (
        <div className="rounded-xl border border-[var(--border)] bg-[var(--surface)] overflow-hidden">
          <ul className="divide-y divide-[var(--border)]">
            {sorted.map((ticket) => {
              const sla = slaCountdown(ticket.slaDeadline);
              return (
                <li key={ticket.id}>
                  <Link
                    href={`/tickets/${ticket.id}`}
                    className="block px-5 py-4 hover:bg-[var(--surface-raised)] transition-colors"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-[var(--text)] truncate">
                          {ticket.title}
                        </p>
                        <p className="mt-1 text-sm text-[var(--text-muted)] line-clamp-1">
                          {ticket.description}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <Badge tone={IMPACT_TONE[ticket.impact]}>{ticket.impact}</Badge>
                        <StatusBadge status={ticket.status} />
                      </div>
                    </div>
                    <p
                      className="mt-2 text-xs"
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
                    </p>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
