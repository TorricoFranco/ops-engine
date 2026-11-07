package com.franco.opsengine.decision.domain;

/**
 * El proveedor no pudo dar un juicio: falló, tardó de más o devolvió algo que no se entiende.
 *
 * <p>Se lanza en lugar de devolver un juicio neutro. Un error no es un "no": si se tratara como
 * una respuesta, el ruteo tomaría ruido por señal. Quien llama manda el ticket a revisión.
 */
public class DecisionUnavailableException extends RuntimeException {

    public DecisionUnavailableException(String message) {
        super(message);
    }

    public DecisionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
