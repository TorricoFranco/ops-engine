const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TOKEN_KEY = "ops-engine.token";
const ROLE_KEY = "ops-engine.role";

export type Role = "AGENT" | "SUPERVISOR" | "ADMIN";

export type TicketStatus =
  | "NUEVO"
  | "EVALUANDO"
  | "ASIGNADO"
  | "EN_PROGRESO"
  | "RESUELTO"
  | "REVISION_REQUERIDA"
  | "INFORMACION_INSUFICIENTE"
  | "CERRADO";

export type Impact = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface Ticket {
  id: string;
  title: string;
  description: string;
  impact: Impact;
  requiredSkill: string;
  status: TicketStatus;
  assigneeId: string | null;
  reviewReason: string | null;
  createdAt: string;
  slaDeadline: string;
}

export interface Judgement {
  suggestedSkill: string;
  skillConfidence: number;
  skillRecognized: number;
  urgency: number;
  urgencyLevels: number;
  multipleCustomersAffected: number;
  serviceUnavailable: number;
}

export interface Candidate {
  employeeId: string;
  name: string;
  eligible: boolean;
  rejectionReason: string | null;
  openTickets: number;
  maxOpenTickets: number;
}

export interface Decision {
  id: string;
  provider: string;
  policyVersion: string;
  outcome: "ASSIGNED" | "SENT_TO_REVIEW" | "NEEDS_MORE_INFORMATION";
  assignedEmployeeId: string | null;
  reviewReason: string | null;
  rationale: string;
  judgement: Judgement | null;
  candidates: Candidate[];
  latencyMs: number;
  decidedAt: string;
}

export interface Employee {
  id: string;
  name: string;
  teamId: string;
  skills: string[];
  maxOpenTickets: number;
  openTickets: number;
  availability: "AVAILABLE" | "UNAVAILABLE";
  clearance: "STANDARD" | "CRITICAL";
}

export interface PendingReview {
  ticketId: string;
  title: string;
  status: TicketStatus;
  slaDeadline: string;
  reviewReason: string | null;
  rationale: string | null;
  suggestedSkill: string | null;
  skillConfidence: number | null;
}

export interface Metrics {
  decisions: number;
  assigned: number;
  sentToReview: number;
  reviewRate: number | null;
  resolutions: number;
  comparable: number;
  overrides: number;
  overrideRate: number | null;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

export const session = {
  token: () => (typeof window === "undefined" ? null : localStorage.getItem(TOKEN_KEY)),
  role: () =>
    typeof window === "undefined" ? null : (localStorage.getItem(ROLE_KEY) as Role | null),
};

export function saveSession(token: string, role: Role) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(ROLE_KEY, role);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = session.token();
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });

  if (!response.ok) {
    let message = response.statusText;
    try {
      const body = await response.json();
      message = body.message ?? message;
    } catch {
      // Una respuesta sin cuerpo JSON, como un 401. Queda el statusText.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const api = {
  login: (username: string, password: string) =>
    request<{ accessToken: string; role: Role; expiresAt: string }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),

  tickets: () => request<Ticket[]>("/api/tickets"),
  ticket: (id: string) => request<Ticket>(`/api/tickets/${id}`),
  decisions: (id: string) => request<Decision[]>(`/api/tickets/${id}/decisions`),
  route: (id: string) => request<unknown>(`/api/tickets/${id}/routing`, { method: "POST" }),

  employees: () => request<Employee[]>("/api/employees"),
  pendingReviews: () => request<PendingReview[]>("/api/review"),
  metrics: () => request<Metrics>("/api/review/metrics"),

  assignFromReview: (
    ticketId: string,
    employeeId: string,
    agreedWithSuggestion: boolean,
    disagreementReason?: string,
  ) =>
    request<unknown>(`/api/review/${ticketId}/assign`, {
      method: "POST",
      body: JSON.stringify({ employeeId, agreedWithSuggestion, disagreementReason }),
    }),
};
