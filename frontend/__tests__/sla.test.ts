import { slaCountdown } from "@/components/domain";

const NOW = new Date("2026-11-09T12:00:00Z").getTime();
const at = (iso: string) => slaCountdown(iso, NOW);

describe("slaCountdown", () => {
  it("cuenta las horas y los minutos que faltan", () => {
    expect(at("2026-11-09T19:30:00Z").text).toBe("vence en 7 h 30 min");
  });

  it("usa días cuando falta más de uno", () => {
    expect(at("2026-11-11T12:00:00Z").text).toBe("vence en 2 d");
  });

  it("usa solo minutos cuando falta menos de una hora", () => {
    expect(at("2026-11-09T12:25:00Z").text).toBe("vence en 25 min");
  });

  it("avisa cuando ya venció en lugar de mostrar un número negativo", () => {
    const sla = at("2026-11-09T11:30:00Z");

    expect(sla.text).toBe("vencido hace 30 min");
    expect(sla.tone).toBe("danger");
  });

  it("marca en ámbar lo que está por vencer", () => {
    // Es la misma ventana que usan las reglas para considerar crítico un ticket.
    expect(at("2026-11-09T12:20:00Z").tone).toBe("warn");
  });

  it("no alarma cuando todavía hay tiempo", () => {
    expect(at("2026-11-09T20:00:00Z").tone).toBe("neutral");
  });
});
