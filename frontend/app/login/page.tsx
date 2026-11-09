"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { api, saveSession } from "@/lib/api";

const DEV_USERS = ["agent", "supervisor", "admin"];

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("supervisor");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const { accessToken, role } = await api.login(username, password);
      saveSession(accessToken, role);
      router.push("/");
    } catch {
      // Mismo mensaje para usuario inexistente y contraseña incorrecta, igual que la API.
      setError("Usuario o contraseña incorrectos");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen grid place-items-center px-6">
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-2.5 mb-8">
          <span
            aria-hidden
            className="w-3 h-3 rounded-sm"
            style={{ background: "var(--accent)" }}
          />
          <h1 className="text-xl font-semibold tracking-tight">ops-engine</h1>
        </div>

        <form
          onSubmit={submit}
          className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 space-y-4"
        >
          <div>
            <label htmlFor="username" className="block text-sm text-[var(--text-muted)] mb-1.5">
              Usuario
            </label>
            <input
              id="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              className="w-full rounded-lg bg-[var(--surface-raised)] border border-[var(--border)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm text-[var(--text-muted)] mb-1.5">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              className="w-full rounded-lg bg-[var(--surface-raised)] border border-[var(--border)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
            />
          </div>

          {error && (
            <p role="alert" className="text-sm" style={{ color: "var(--danger)" }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-lg px-3 py-2 text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "var(--accent)", color: "#06101f" }}
          >
            {busy ? "Entrando…" : "Entrar"}
          </button>
        </form>

        <p className="mt-4 text-xs text-[var(--text-muted)] leading-relaxed">
          En el perfil <code className="font-mono">dev</code> existen los usuarios{" "}
          {DEV_USERS.join(", ")}. La contraseña está en <code className="font-mono">DevUserSeeder</code>{" "}
          y esos usuarios no existen en ningún otro perfil.
        </p>
      </div>
    </div>
  );
}
