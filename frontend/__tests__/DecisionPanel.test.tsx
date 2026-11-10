import { render, screen } from "@testing-library/react";
import { DecisionPanel } from "@/components/DecisionPanel";
import type { Decision, Employee } from "@/lib/api";

const EMPLOYEES: Employee[] = [
  {
    id: "e1",
    name: "Diego Ramírez",
    teamId: "t1",
    skills: ["INFRASTRUCTURE"],
    maxOpenTickets: 4,
    openTickets: 2,
    availability: "AVAILABLE",
    clearance: "CRITICAL",
  },
];

/**
 * Los campos ausentes se omiten en lugar de ponerse en nulo: la API está configurada con
 * `default-property-inclusion: non_null`, así que un campo sin valor no viaja. Escribir
 * `null` acá probaba una respuesta que el backend nunca manda.
 */
function decision(overrides: Partial<Decision> = {}): Decision {
  return {
    id: "d1b2c3d4-0000-0000-0000-000000000000",
    provider: "jev",
    policyVersion: "2026-11-09.1",
    outcome: "ASSIGNED",
    assignedEmployeeId: "e1",
    rationale: "Diego Ramírez tiene la competencia INFRASTRUCTURE",
    judgement: {
      suggestedSkill: "INFRASTRUCTURE",
      skillConfidence: 1,
      skillRecognized: 0.98,
      urgency: 3.9,
      urgencyLevels: 5,
      multipleCustomersAffected: 0.87,
      serviceUnavailable: 0.91,
    },
    candidates: [
      {
        employeeId: "e1",
        name: "Diego Ramírez",
        eligible: true,
        openTickets: 2,
        maxOpenTickets: 4,
      },
      {
        employeeId: "e2",
        name: "Federico Ruiz",
        eligible: false,
        rejectionReason: "INSUFFICIENT_CLEARANCE",
        openTickets: 0,
        maxOpenTickets: 4,
      },
    ],
    latencyMs: 272,
    decidedAt: "2026-11-09T14:33:22Z",
    ...overrides,
  };
}

describe("DecisionPanel", () => {
  it("muestra las probabilidades del modelo como porcentajes", () => {
    render(<DecisionPanel decision={decision()} employees={EMPLOYEES} />);

    expect(screen.getByText("Confianza en esa competencia")).toBeInTheDocument();
    expect(screen.getByText("100%")).toBeInTheDocument();
    // La urgencia normalizada también redondea a 98%, así que hay dos.
    expect(screen.getAllByText("98%")).toHaveLength(2);
    expect(screen.getByText("87%")).toBeInTheDocument();
  });

  it("nombra al empleado asignado en lugar de mostrar su id", () => {
    render(<DecisionPanel decision={decision()} employees={EMPLOYEES} />);

    // Aparece como asignado y otra vez en la lista de candidatos.
    expect(screen.getAllByText("Diego Ramírez").length).toBeGreaterThan(0);
    expect(screen.queryByText("e1")).not.toBeInTheDocument();
  });

  it("explica en castellano por qué quedó afuera cada candidato", () => {
    // Mostrar el enum crudo obligaría a conocer el código para entender la pantalla.
    render(<DecisionPanel decision={decision()} employees={EMPLOYEES} />);

    expect(screen.getByText("sin habilitación para críticos")).toBeInTheDocument();
    expect(screen.queryByText("INSUFFICIENT_CLEARANCE")).not.toBeInTheDocument();
  });

  it("muestra la versión de reglas con la que se decidió", () => {
    render(<DecisionPanel decision={decision()} employees={EMPLOYEES} />);

    expect(screen.getByText(/2026-11-09\.1/)).toBeInTheDocument();
  });

  it("explica la falta de juicio cuando el proveedor no respondió", () => {
    render(
      <DecisionPanel
        decision={decision({
          judgement: undefined,
          outcome: "SENT_TO_REVIEW",
          assignedEmployeeId: undefined,
          reviewReason: "PROVIDER_UNAVAILABLE",
          candidates: [],
        })}
        employees={EMPLOYEES}
      />,
    );

    // Aparece como motivo de la derivación y en la explicación del panel del modelo.
    expect(screen.getAllByText(/el proveedor no respondió/i).length).toBeGreaterThan(0);
    expect(
      screen.getByText(/El ticket fue a revisión en lugar de rutearse con datos inventados/),
    ).toBeInTheDocument();
  });

  it("aclara que lo del modelo son estimaciones y no decisiones", () => {
    // Es la tesis del proyecto: si la pantalla no lo dice, se lee como que decidió la IA.
    render(<DecisionPanel decision={decision()} employees={EMPLOYEES} />);

    expect(screen.getByText(/Son estimaciones, no decisiones/)).toBeInTheDocument();
  });
});
