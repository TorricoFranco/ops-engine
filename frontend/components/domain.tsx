import type { Decision, TicketStatus } from "@/lib/api";
import { Badge } from "./ui";

const STATUS_LABEL: Record<TicketStatus, { text: string; tone: "ok" | "warn" | "danger" | "neutral" | "model" }> = {
  NUEVO: { text: "En cola", tone: "neutral" },
  EVALUANDO: { text: "Evaluando", tone: "model" },
  ASIGNADO: { text: "Asignado", tone: "ok" },
  EN_PROGRESO: { text: "En progreso", tone: "ok" },
  RESUELTO: { text: "Resuelto", tone: "ok" },
  REVISION_REQUERIDA: { text: "Espera a una persona", tone: "warn" },
  INFORMACION_INSUFICIENTE: { text: "Falta información", tone: "warn" },
  CERRADO: { text: "Cerrado", tone: "neutral" },
};

export function StatusBadge({ status }: { status: TicketStatus }) {
  const { text, tone } = STATUS_LABEL[status] ?? { text: status, tone: "neutral" as const };
  return <Badge tone={tone}>{text}</Badge>;
}

export const REJECTION_LABEL: Record<string, string> = {
  MISSING_SKILL: "no tiene la competencia",
  UNAVAILABLE: "no está disponible",
  AT_CAPACITY: "sin capacidad",
  INSUFFICIENT_CLEARANCE: "sin habilitación para críticos",
};

export const REVIEW_LABEL: Record<string, string> = {
  PROVIDER_UNAVAILABLE: "el proveedor no respondió",
  SKILL_NOT_RECOGNIZED: "no corresponde a ninguna área",
  LOW_CONFIDENCE: "la competencia no supera el umbral",
  NO_ELIGIBLE_AGENT: "nadie cumple los requisitos",
};

export function OutcomeBadge({ outcome }: { outcome: Decision["outcome"] }) {
  if (outcome === "ASSIGNED") return <Badge tone="ok">Asignado</Badge>;
  if (outcome === "NEEDS_MORE_INFORMATION") return <Badge tone="warn">Faltan datos</Badge>;
  return <Badge tone="warn">A revisión</Badge>;
}

/** Cuánto falta para el SLA, en palabras. Negativo si ya venció. */
export function slaCountdown(deadline: string, now: number = Date.now()) {
  const diffMs = new Date(deadline).getTime() - now;
  const overdue = diffMs < 0;
  const minutes = Math.floor(Math.abs(diffMs) / 60000);
  const hours = Math.floor(minutes / 60);

  const text =
    hours >= 24
      ? `${Math.floor(hours / 24)} d`
      : hours >= 1
        ? `${hours} h ${minutes % 60} min`
        : `${minutes} min`;

  return {
    text: overdue ? `vencido hace ${text}` : `vence en ${text}`,
    tone: overdue ? ("danger" as const) : minutes < 30 ? ("warn" as const) : ("neutral" as const),
  };
}
