import { skillName } from "@/components/domain";
import type { Skill } from "@/lib/api";

const SKILLS: Skill[] = [
  { code: "BILLING", name: "Facturación" },
  { code: "INFRASTRUCTURE", name: "Infraestructura" },
];

describe("skillName", () => {
  it("devuelve el nombre que trae el catálogo para el código", () => {
    expect(skillName(SKILLS, "BILLING")).toBe("Facturación");
  });

  it("muestra el código cuando el catálogo no trae el área, para que se note", () => {
    expect(skillName(SKILLS, "ON_CALL")).toBe("ON_CALL");
  });

  it("muestra un guion cuando no hay área", () => {
    expect(skillName(SKILLS, null)).toBe("—");
    expect(skillName(SKILLS, undefined)).toBe("—");
  });

  it("muestra el código mientras el catálogo todavía no llegó", () => {
    expect(skillName(null, "BILLING")).toBe("BILLING");
  });
});
