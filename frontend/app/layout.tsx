import type { Metadata } from "next";
import { Suspense } from "react";
import "./globals.css";
import { Nav } from "@/components/Nav";

export const metadata: Metadata = {
  title: "ops-engine · consola de operaciones",
  description:
    "Ruteo de tickets de soporte donde la IA estima y el código decide, con cada decisión explicada.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className="min-h-screen antialiased">
        {/* Lee la ruta activa con un hook dinámico, que Next no puede prerenderizar.
            El fallback reserva el alto de la barra para que no salte al aparecer. */}
        <Suspense fallback={<div className="h-14 border-b border-[var(--border)]" />}>
          <Nav />
        </Suspense>
        <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>
      </body>
    </html>
  );
}
