"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, session } from "@/lib/api";

/**
 * Carga datos y maneja los dos casos que se repiten en todas las pantallas: todavía no hay
 * sesión, y la sesión venció mientras se miraba la página.
 *
 * @param key cambia cuando hay que volver a cargar, por ejemplo el id de la ruta. Es un
 *            string y no un array de dependencias porque la regla de hooks exige que la
 *            lista del efecto sea un literal.
 */
export function useApi<T>(load: () => Promise<T>, key = "") {
  const router = useRouter();
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);

  // La función de carga se redefine en cada render. Guardarla en una ref evita que el efecto
  // de carga dependa de su identidad y se vuelva a disparar en bucle.
  //
  // La ref se escribe en un efecto y no durante el render. Está declarado antes que el de
  // carga, así que para cuando ese corre la referencia ya quedó actualizada.
  const loadRef = useRef(load);

  useEffect(() => {
    loadRef.current = load;
  });

  useEffect(() => {
    if (!session.token()) {
      router.replace("/login");
      return;
    }

    // Si la ruta cambia mientras la respuesta viaja, se descarta: escribirla pisaría los
    // datos de la pantalla nueva.
    let cancelled = false;

    loadRef.current()
      .then((result) => {
        if (cancelled) return;
        setData(result);
        setError(null);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/login");
          return;
        }
        setError(
          e instanceof ApiError
            ? e.message
            : "No se pudo conectar con la API. ¿Está levantada en el puerto 8080?",
        );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [key, attempt, router]);

  const reload = useCallback(() => {
    setLoading(true);
    setAttempt((previous) => previous + 1);
  }, []);

  return { data, error, loading, reload };
}
