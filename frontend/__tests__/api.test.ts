import { api, saveSession, clearSession } from "@/lib/api";

describe("el cliente de la API", () => {
  let sent: RequestInit | undefined;

  beforeEach(() => {
    clearSession();
    sent = undefined;
    global.fetch = jest.fn((_url: string, init: RequestInit) => {
      sent = init;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ accessToken: "t", role: "AGENT", expiresAt: "" }),
      });
    }) as unknown as typeof fetch;
  });

  function authorizationHeader() {
    return (sent?.headers as Record<string, string> | undefined)?.Authorization;
  }

  it("no manda el token guardado al iniciar sesión", async () => {
    // Un token vencido viaja igual en la cabecera y el resource server lo rechaza con 401
    // antes de llegar al controlador, aunque el endpoint sea público. Sin esto, una sesión
    // vencida dejaba imposible volver a entrar: la pantalla decía "usuario o contraseña
    // incorrectos" con las credenciales correctas.
    saveSession("token-vencido", "AGENT");

    await api.login("supervisor", "una-clave");

    expect(authorizationHeader()).toBeUndefined();
  });

  it("manda el token en el resto de las llamadas", async () => {
    saveSession("token-valido", "AGENT");

    await api.tickets();

    expect(authorizationHeader()).toBe("Bearer token-valido");
  });

  it("no inventa una cabecera cuando no hay sesión", async () => {
    await api.tickets();

    expect(authorizationHeader()).toBeUndefined();
  });
});
