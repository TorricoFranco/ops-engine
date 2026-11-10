import { readFileSync } from "node:fs";
import { join } from "node:path";
import { REJECTION_LABEL, REVIEW_LABEL } from "@/components/domain";

/**
 * Ata el contrato publicado a lo que la pantalla sabe traducir.
 *
 * `tsc` ya obliga a que los diccionarios cubran las uniones generadas, pero eso solo vale
 * si alguien regeneró los tipos. Este test lee el OpenAPI versionado, así que también falla
 * cuando el contrato cambió y `api.generated.ts` quedó viejo.
 */
const spec = JSON.parse(readFileSync(join(__dirname, "..", "openapi.json"), "utf8"));

function enumOf(schema: string, property: string): string[] {
  const values = spec.components?.schemas?.[schema]?.properties?.[property]?.enum;
  if (!values) {
    throw new Error(`${schema}.${property} dejó de declarar sus valores en el OpenAPI`);
  }
  return values;
}

describe("el contrato de la API y los textos de la pantalla", () => {
  it("traduce todos los motivos de descarte que el backend puede devolver", () => {
    expect(Object.keys(REJECTION_LABEL).sort()).toEqual(
      enumOf("DecisionCandidateView", "rejectionReason").sort(),
    );
  });

  it("traduce todos los motivos de derivación a una persona", () => {
    expect(Object.keys(REVIEW_LABEL).sort()).toEqual(
      enumOf("DecisionView", "reviewReason").sort(),
    );
  });

  it("publica los valores de los enums y no texto libre", () => {
    // Con `type: string` a secas, el generador produce `string` y agregar un motivo nuevo
    // deja de romper el build: la pantalla pasa a mostrar el enum crudo.
    expect(enumOf("TicketResponse", "status")).toContain("INFORMACION_INSUFICIENTE");
    expect(enumOf("DecisionView", "outcome")).toContain("NEEDS_MORE_INFORMATION");
  });

  it("publica el catálogo de áreas con su nombre para la consola", () => {
    expect(spec.components.schemas.SkillView.properties).toHaveProperty("name");
    expect(spec.components.schemas.SkillView.properties).toHaveProperty("code");
  });

  it("no mezcla los dos esquemas de candidatos", () => {
    // springdoc indexa por nombre simple: con los dos records llamados CandidateView, uno
    // pisaba al otro y el contrato describía un endpoint con la forma del otro.
    expect(spec.components.schemas.RoutingCandidateView).toBeDefined();
    expect(spec.components.schemas.DecisionCandidateView).toBeDefined();
  });
});
