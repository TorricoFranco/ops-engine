import type { Decision, Employee } from "@/lib/api";
import { Bar, Badge, Card, Mono } from "./ui";
import { OutcomeBadge, REJECTION_LABEL, REVIEW_LABEL } from "./domain";

/**
 * La pantalla que explica el sistema.
 *
 * <p>Está partida en dos a propósito: arriba lo que estimó el modelo, abajo lo que decidió el
 * código. Esa separación es la tesis del proyecto, así que la interfaz la hace literal en
 * lugar de mezclar todo en una sola lista.
 */
export function DecisionPanel({
  decision,
  employees,
}: {
  decision: Decision;
  employees: Employee[];
}) {
  const nameOf = (id: string | null) =>
    employees.find((employee) => employee.id === id)?.name ?? "—";

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card
        title="Lo que estimó el modelo"
        subtitle={`proveedor ${decision.provider} · ${decision.latencyMs} ms`}
        accent="var(--model)"
      >
        {decision.judgement ? (
          <div className="space-y-1">
            <div className="flex items-baseline justify-between gap-3 pb-3 mb-2 border-b border-[var(--border)]">
              <span className="text-xs uppercase tracking-wide text-[var(--text-muted)]">
                Competencia elegida
              </span>
              <span className="font-mono text-sm text-[var(--model)]">
                {decision.judgement.suggestedSkill}
              </span>
            </div>

            <Bar
              label="Confianza en esa competencia"
              value={decision.judgement.skillConfidence}
              hint="Probabilidad de la competencia elegida entre todas las opciones"
            />
            <Bar
              label="Es un caso nuestro"
              value={decision.judgement.skillRecognized}
              hint="Un choice siempre elige una opción; esta pregunta detecta cuando ninguna encaja"
            />
            <Bar
              label="Urgencia"
              value={
                decision.judgement.urgencyLevels > 1
                  ? decision.judgement.urgency / (decision.judgement.urgencyLevels - 1)
                  : 0
              }
              hint="Cuánto le impide operar al cliente, en la escala descrita al modelo"
            />
            <Bar
              label="Varios afectados"
              value={decision.judgement.multipleCustomersAffected}
            />
            <Bar label="Servicio caído" value={decision.judgement.serviceUnavailable} />

            <p className="pt-3 text-xs leading-relaxed text-[var(--text-muted)]">
              Son estimaciones, no decisiones. El modelo no sabe quién está disponible, quién
              tiene permiso ni cuándo vence el SLA.
            </p>
          </div>
        ) : (
          <p className="text-sm text-[var(--text-muted)]">
            No hubo juicio: el proveedor no respondió. El ticket fue a revisión en lugar de
            rutearse con datos inventados.
          </p>
        )}
      </Card>

      <Card
        title="Lo que decidió el código"
        subtitle={`reglas ${decision.policyVersion}`}
        accent="var(--accent)"
      >
        <div className="flex flex-wrap items-center gap-2">
          <OutcomeBadge outcome={decision.outcome} />
          {decision.outcome === "ASSIGNED" && (
            <span className="text-sm text-[var(--text)]">
              {nameOf(decision.assignedEmployeeId)}
            </span>
          )}
          {decision.reviewReason && (
            <span className="text-sm text-[var(--text-muted)]">
              {REVIEW_LABEL[decision.reviewReason] ?? decision.reviewReason}
            </span>
          )}
        </div>

        <p className="mt-3 text-sm leading-relaxed text-[var(--text)]">{decision.rationale}</p>

        {decision.candidates.length > 0 ? (
          <div className="mt-5">
            <p className="text-xs uppercase tracking-wide text-[var(--text-muted)] mb-2">
              Candidatos evaluados
            </p>
            <ul className="divide-y divide-[var(--border)]">
              {decision.candidates.map((candidate) => (
                <li
                  key={candidate.employeeId}
                  className="flex items-center justify-between gap-3 py-2"
                >
                  <span className="flex items-center gap-2 min-w-0">
                    <span
                      aria-hidden
                      className="w-1.5 h-1.5 rounded-full shrink-0"
                      style={{
                        background: candidate.eligible ? "var(--ok)" : "var(--text-muted)",
                      }}
                    />
                    <span
                      className={`text-sm truncate ${
                        candidate.eligible ? "text-[var(--text)]" : "text-[var(--text-muted)]"
                      }`}
                    >
                      {candidate.name}
                    </span>
                  </span>
                  <span className="flex items-center gap-2 shrink-0">
                    <span className="font-mono text-xs text-[var(--text-muted)]">
                      {candidate.openTickets}/{candidate.maxOpenTickets}
                    </span>
                    {candidate.eligible ? (
                      <Badge tone="ok">elegible</Badge>
                    ) : (
                      <Badge>
                        {REJECTION_LABEL[candidate.rejectionReason ?? ""] ??
                          candidate.rejectionReason}
                      </Badge>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <p className="mt-5 text-xs text-[var(--text-muted)]">
            No se evaluaron candidatos: la decisión se tomó antes de llegar a ese paso.
          </p>
        )}

        <p className="mt-5 text-xs text-[var(--text-muted)]">
          Decisión <Mono>{decision.id.slice(0, 8)}</Mono> del{" "}
          {new Date(decision.decidedAt).toLocaleString("es-AR")}
        </p>
      </Card>
    </div>
  );
}
