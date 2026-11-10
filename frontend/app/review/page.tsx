"use client";

import Link from "next/link";
import { api, type Employee, type PendingReview, type Skill } from "@/lib/api";
import { useApi } from "@/components/useApi";
import { Badge, Card, Empty, ErrorNote, Loading } from "@/components/ui";
import { REVIEW_LABEL, skillName, slaCountdown } from "@/components/domain";
import { ResolveForm } from "@/components/ResolveForm";

export default function ReviewPage() {
  const { data, error, loading, reload } = useApi<[PendingReview[], Employee[], Skill[]]>(() =>
    Promise.all([api.pendingReviews(), api.employees(), api.skills()]),
  );

  if (loading) return <Loading />;
  if (error) return <ErrorNote message={error} />;
  if (!data) return null;

  const [pending, employees, skills] = data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Bandeja de revisión</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          Lo que el sistema no se animó a decidir solo. Al resolver se registra si coincidiste
          con lo que sugirió, y de ahí sale la tasa de override.
        </p>
      </div>

      {pending.length === 0 ? (
        <Empty>
          No hay nada esperando. El sistema resolvió solo todo lo que entró.
        </Empty>
      ) : (
        <div className="space-y-4">
          {pending.map((item) => {
            const sla = slaCountdown(item.slaDeadline);
            return (
              <Card key={item.ticketId} accent="var(--warn)">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <Link
                      href={`/tickets/${item.ticketId}`}
                      className="text-sm font-medium hover:text-[var(--accent)] transition-colors"
                    >
                      {item.title}
                    </Link>
                    <p className="mt-1 text-sm text-[var(--text-muted)]">
                      {item.reviewReason
                        ? (REVIEW_LABEL[item.reviewReason] ?? item.reviewReason)
                        : "sin decisión registrada"}
                    </p>
                    {item.rationale && (
                      <p className="mt-1 text-xs text-[var(--text-muted)]">{item.rationale}</p>
                    )}
                  </div>
                  <div className="flex flex-col items-end gap-2 shrink-0">
                    {item.suggestedSkill && (
                      <Badge tone="model">
                        sugirió {skillName(skills, item.suggestedSkill)}
                        {item.skillConfidence != null &&
                          ` · ${Math.round(item.skillConfidence * 100)}%`}
                      </Badge>
                    )}
                    <span
                      className="text-xs"
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
                  </div>
                </div>

                <ResolveForm
                  ticketId={item.ticketId}
                  employees={employees}
                  skills={skills}
                  hasSuggestion={item.suggestedSkill !== null}
                  onResolved={reload}
                />
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
