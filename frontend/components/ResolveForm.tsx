"use client";

import { useState } from "react";
import { api, type Employee } from "@/lib/api";

/**
 * Resolver una revisión.
 *
 * <p>Cuando el supervisor marca que no coincidió, el motivo pasa a ser obligatorio y el botón
 * se bloquea hasta que lo escriba. La API lo rechazaría igual, pero pedirlo acá explica por
 * qué hace falta en lugar de devolver un error después.
 */
export function ResolveForm({
  ticketId,
  employees,
  hasSuggestion,
  onResolved,
}: {
  ticketId: string;
  employees: Employee[];
  hasSuggestion: boolean;
  onResolved: () => void;
}) {
  const [employeeId, setEmployeeId] = useState("");
  const [agreed, setAgreed] = useState(true);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const needsReason = hasSuggestion && !agreed;
  const canSubmit = employeeId !== "" && (!needsReason || reason.trim() !== "");

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.assignFromReview(
        ticketId,
        employeeId,
        hasSuggestion ? agreed : false,
        needsReason ? reason.trim() : undefined,
      );
      onResolved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo resolver");
    } finally {
      setBusy(false);
    }
  }

  const available = employees.filter(
    (employee) => employee.availability === "AVAILABLE" && employee.openTickets < employee.maxOpenTickets,
  );

  return (
    <form onSubmit={submit} className="mt-5 pt-5 border-t border-[var(--border)] space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={employeeId}
          onChange={(event) => setEmployeeId(event.target.value)}
          aria-label="Asignar a"
          className="rounded-lg bg-[var(--surface-raised)] border border-[var(--border)] px-3 py-1.5 text-sm outline-none focus:border-[var(--accent)]"
        >
          <option value="">Asignar a…</option>
          {available.map((employee) => (
            <option key={employee.id} value={employee.id}>
              {employee.name} · {employee.skills.join(", ")} · {employee.openTickets}/
              {employee.maxOpenTickets}
            </option>
          ))}
        </select>

        {hasSuggestion && (
          <label className="flex items-center gap-2 text-sm text-[var(--text-muted)]">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(event) => setAgreed(event.target.checked)}
              className="accent-[var(--accent)]"
            />
            Coincido con lo que sugirió el sistema
          </label>
        )}

        <button
          type="submit"
          disabled={busy || !canSubmit}
          className="ml-auto rounded-lg px-3 py-1.5 text-sm font-medium transition-opacity disabled:opacity-40"
          style={{ background: "var(--accent)", color: "#06101f" }}
        >
          {busy ? "Resolviendo…" : "Resolver"}
        </button>
      </div>

      {needsReason && (
        <input
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder="¿Por qué no coincidís? El motivo es obligatorio"
          aria-label="Motivo del desacuerdo"
          className="w-full rounded-lg bg-[var(--surface-raised)] border border-[var(--border)] px-3 py-1.5 text-sm outline-none focus:border-[var(--accent)]"
        />
      )}

      {error && (
        <p role="alert" className="text-sm" style={{ color: "var(--danger)" }}>
          {error}
        </p>
      )}
    </form>
  );
}
