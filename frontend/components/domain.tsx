import type {
  DecisionOutcome,
  RejectionReason,
  ReviewReason,
  Skill,
  TicketStatus,
} from "@/lib/api";
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
  const { text, tone } = STATUS_LABEL[status];
  return <Badge tone={tone}>{text}</Badge>;
}

/**
 * Los diccionarios están tipados sobre las uniones que salen del OpenAPI, no sobre
 * `string`. Esa es la diferencia que importa: si el backend agrega un motivo, acá falta una
 * clave y `tsc` falla. Con `Record<string, string>` compilaba igual y la pantalla mostraba
 * el enum en inglés, que es el error que esto viene a evitar (ADR-013).
 */
export const REJECTION_LABEL: Record<RejectionReason, string> = {
  MISSING_SKILL: "no tiene la competencia",
  UNAVAILABLE: "no está disponible",
  AT_CAPACITY: "sin capacidad",
  INSUFFICIENT_CLEARANCE: "sin habilitación para críticos",
};

export const REVIEW_LABEL: Record<ReviewReason, string> = {
  PROVIDER_UNAVAILABLE: "el proveedor no respondió",
  SKILL_NOT_RECOGNIZED: "no corresponde a ninguna área",
  LOW_CONFIDENCE: "la competencia no supera el umbral",
  NO_ELIGIBLE_AGENT: "nadie cumple los requisitos",
  ROUTING_FAILED: "el ruteo falló varias veces",
};

const OUTCOME_LABEL: Record<DecisionOutcome, { text: string; tone: "ok" | "warn" }> = {
  ASSIGNED: { text: "Asignado", tone: "ok" },
  NEEDS_MORE_INFORMATION: { text: "Faltan datos", tone: "warn" },
  SENT_TO_REVIEW: { text: "A revisión", tone: "warn" },
};

/**
 * Nombre de un área para la pantalla. Los nombres vienen de `GET /api/skills`: agregar un área
 * es una fila en la base y no un cambio acá. Si el catálogo todavía no la trae, se muestra el
 * código: queda a la vista y se corrige con la fila, en lugar de esconderse detrás de un texto
 * inventado.
 */
export function skillName(skills: Skill[] | null, code: string | null | undefined): string {
  if (!code) return "—";
  return skills?.find((skill) => skill.code === code)?.name ?? code;
}

export function OutcomeBadge({ outcome }: { outcome: DecisionOutcome }) {
  const { text, tone } = OUTCOME_LABEL[outcome];
  return <Badge tone={tone}>{text}</Badge>;
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
