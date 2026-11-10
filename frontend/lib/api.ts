import type { components } from "./api.generated";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TOKEN_KEY = "ops-engine.token";
const ROLE_KEY = "ops-engine.role";

/**
 * Los tipos de la API no se escriben acá: salen del OpenAPI que publica el backend.
 *
 * `npm run types:api` regenera `api.generated.ts` desde la aplicación corriendo. Lo que
 * sigue son nombres cortos para esos esquemas, para no arrastrar
 * `components["schemas"]["..."]` por todo el código.
 *
 * El motivo de hacerlo así está en ADR-013: cuando las uniones de valores se escriben a
 * mano, agregar un estado o un motivo en el backend no rompe nada, y la pantalla termina
 * mostrando el enum crudo en inglés.
 */
type Schemas = components["schemas"];

/**
 * Marca como presentes todos los campos del esquema menos los que se nombran.
 *
 * <p>springdoc no declara `required`, así que el OpenAPI deja todo opcional y sin esto cada
 * lectura arrastraría un `undefined` que no existe. Lo que de verdad puede faltar es lo que
 * el backend deja en nulo, y la API los omite del JSON porque está configurada con
 * `default-property-inclusion: non_null`. Esa lista, que es conocimiento del dominio, se
 * escribe acá; el resto de los campos lo sigue aportando el esquema.
 */
type Present<T, Ausentes extends keyof T> = Required<Omit<T, Ausentes>> & Pick<T, Ausentes>;

/** Sin asignar todavía, y sin motivo de revisión mientras no haya ido a revisión. */
export type Ticket = Present<Schemas["TicketResponse"], "assigneeId" | "reviewReason">;

export type Judgement = Required<Schemas["JudgementView"]>;

/** Sin motivo de descarte cuando el candidato era elegible. */
export type Candidate = Present<Schemas["DecisionCandidateView"], "rejectionReason">;

/**
 * Sin juicio cuando el proveedor no respondió: una falla no se convierte en juicio.
 *
 * <p>Los dos campos anidados se reescriben porque `Required` no entra en las propiedades de
 * un objeto hijo: sin esto, el juicio llegaría con todos sus números en opcional.
 */
export type Decision = Present<
  Omit<Schemas["DecisionView"], "judgement" | "candidates">,
  "assignedEmployeeId" | "reviewReason"
> & {
  judgement?: Judgement;
  candidates: Candidate[];
};

export type Employee = Required<Schemas["EmployeeResponse"]>;

/** Todo lo que viene de la decisión falta si el ticket llegó a revisión sin una. */
export type PendingReview = Present<
  Schemas["PendingView"],
  "reviewReason" | "rationale" | "suggestedSkill" | "skillConfidence"
>;

/** Las tasas son cocientes: no hay número mientras el divisor sea cero. */
export type Metrics = Present<Schemas["MetricsView"], "reviewRate" | "overrideRate">;

/**
 * Las uniones de valores, sacadas de los campos donde aparecen.
 *
 * `NonNullable` porque en el esquema todo campo es opcional: springdoc no marca nada como
 * requerido, y acá lo que interesa es el conjunto de valores, no si el campo viene.
 */
export type TicketStatus = NonNullable<Ticket["status"]>;
export type Impact = NonNullable<Ticket["impact"]>;
export type DecisionOutcome = NonNullable<Decision["outcome"]>;
export type ReviewReason = NonNullable<Decision["reviewReason"]>;
export type RejectionReason = NonNullable<Candidate["rejectionReason"]>;

/** El rol no viaja en ningún esquema: sale del claim del token, no del cuerpo. */
export type Role = "AGENT" | "SUPERVISOR" | "ADMIN";

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

/**
 * @param anonymous no manda el token aunque haya uno guardado.
 *
 * Hace falta para el login. Un token vencido en el navegador viaja igual en la cabecera, y
 * el resource server lo rechaza con 401 antes de que la petición llegue al controlador,
 * aunque ese endpoint sea público. El resultado era que, una vez vencida la sesión, no se
 * podía volver a entrar nunca: la pantalla decía "usuario o contraseña incorrectos" con las
 * credenciales correctas, y la única salida era borrar los datos del sitio a mano.
 */
async function request<T>(path: string, init: RequestInit = {}, anonymous = false): Promise<T> {
  const token = anonymous ? null : session.token();
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
    request<{ accessToken: string; role: Role; expiresAt: string }>(
      "/api/auth/login",
      { method: "POST", body: JSON.stringify({ username, password }) },
      true,
    ),

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
