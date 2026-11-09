"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearSession, session, type Role } from "@/lib/api";

const LINKS = [
  { href: "/", label: "Panel" },
  { href: "/tickets", label: "Tickets" },
  { href: "/review", label: "Revisión" },
];

export function Nav() {
  const pathname = usePathname();
  const router = useRouter();
  const [role, setRole] = useState<Role | null>(null);

  // El rol vive en localStorage, que no existe durante el render del servidor.
  useEffect(() => setRole(session.role()), [pathname]);

  if (pathname === "/login") {
    return null;
  }

  return (
    <header className="border-b border-[var(--border)] bg-[var(--surface)]">
      <div className="mx-auto max-w-6xl px-6 h-14 flex items-center gap-8">
        <Link href="/" className="flex items-center gap-2.5 shrink-0">
          <span
            aria-hidden
            className="w-2.5 h-2.5 rounded-sm"
            style={{ background: "var(--accent)" }}
          />
          <span className="font-semibold tracking-tight">ops-engine</span>
        </Link>

        <nav className="flex gap-1 text-sm">
          {LINKS.map((link) => {
            const active =
              link.href === "/" ? pathname === "/" : pathname.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`px-3 py-1.5 rounded-md transition-colors ${
                  active
                    ? "bg-[var(--surface-raised)] text-[var(--text)]"
                    : "text-[var(--text-muted)] hover:text-[var(--text)]"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="ml-auto flex items-center gap-3 text-sm">
          {role && <span className="text-[var(--text-muted)]">{role.toLowerCase()}</span>}
          <button
            onClick={() => {
              clearSession();
              router.push("/login");
            }}
            className="text-[var(--text-muted)] hover:text-[var(--text)] transition-colors"
          >
            Salir
          </button>
        </div>
      </div>
    </header>
  );
}
