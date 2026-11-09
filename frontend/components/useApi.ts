"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError, session } from "@/lib/api";

/**
 * Carga datos y maneja los dos casos que se repiten en todas las pantallas: todavía no hay
 * sesión, y la sesión venció mientras se miraba la página.
 */
export function useApi<T>(load: () => Promise<T>, deps: unknown[] = []) {
  const router = useRouter();
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const run = useCallback(async () => {
    if (!session.token()) {
      router.replace("/login");
      return;
    }
    setLoading(true);
    try {
      setData(await load());
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        router.replace("/login");
        return;
      }
      setError(
        e instanceof ApiError
          ? e.message
          : "No se pudo conectar con la API. ¿Está levantada en el puerto 8080?",
      );
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    void run();
  }, [run]);

  return { data, error, loading, reload: run };
}
