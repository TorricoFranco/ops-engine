/**
 * Baja el contrato de la aplicación corriendo y lo guarda formateado.
 *
 * Formateado a propósito: el archivo se versiona para que un cambio de API aparezca como
 * diff en el pull request, y en una sola línea eso no se puede leer. Las claves se ordenan
 * para que dos corridas del mismo contrato den el mismo archivo.
 */
const url = process.env.OPENAPI_URL ?? "http://localhost:8080/v3/api-docs";

const response = await fetch(url).catch((cause) => {
  throw new Error(`No se pudo leer ${url}. ¿Está la aplicación levantada?`, { cause });
});

if (!response.ok) {
  throw new Error(`${url} respondió ${response.status}`);
}

const ordered = (value) =>
  Array.isArray(value)
    ? value.map(ordered)
    : value && typeof value === "object"
      ? Object.fromEntries(Object.keys(value).sort().map((key) => [key, ordered(value[key])]))
      : value;

const { writeFile } = await import("node:fs/promises");
await writeFile("openapi.json", JSON.stringify(ordered(await response.json()), null, 2) + "\n");
console.log("openapi.json actualizado");
