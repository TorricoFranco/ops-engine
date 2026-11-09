package com.franco.opsengine.api;

import com.franco.opsengine.employee.application.EmployeeNotFoundException;
import com.franco.opsengine.team.application.TeamNotFoundException;
import com.franco.opsengine.ticket.application.TicketNotFoundException;
import com.franco.opsengine.ticket.domain.InvalidTicketTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Único punto donde las excepciones de dominio y de aplicación se traducen a HTTP.
 * Los controladores no capturan excepciones.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({TicketNotFoundException.class, EmployeeNotFoundException.class, TeamNotFoundException.class})
    ResponseEntity<ApiError> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", e.getMessage()));
    }

    /**
     * Una ruta que no existe.
     *
     * <p>Sin este handler el catch-all la convierte en 500, y un 500 manda a buscar el
     * problema del lado del servidor cuando lo único que pasó es que la URL está mal. Se
     * notó al mover Actuator a su propio puerto: pedir /actuator/health en el puerto de la
     * API dejó de tener handler y la respuesta pasó a ser un error del servidor.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", "no existe esa ruta"));
    }

    /**
     * Un usuario autenticado sin el rol necesario. Sin este handler, el catch-all de abajo
     * lo convertiría en 500: los pedidos anónimos ya los rechaza el filtro con 401.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "no tenés permiso para esta operación"));
    }

    @ExceptionHandler(InvalidTicketTransitionException.class)
    ResponseEntity<ApiError> handleInvalidTransition(InvalidTicketTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    ResponseEntity<ApiError> handleInvalidInput(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", "validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception e) {
        // El detalle queda en el log, no en la respuesta.
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error", "unexpected error"));
    }
}
