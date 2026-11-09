package com.franco.opsengine.ticket.domain;

/**
 * Estados del ticket. Las transiciones permitidas están en {@link #canTransitionTo(TicketStatus)}
 * y reflejan la sección 4 de docs/plan.md.
 */
public enum TicketStatus {
    NUEVO,
    EVALUANDO,
    ASIGNADO,
    EN_PROGRESO,
    RESUELTO,
    REVISION_REQUERIDA,
    /** Falta información del cliente para poder trabajarlo. No consume tiempo de nadie. */
    INFORMACION_INSUFICIENTE,
    CERRADO;

    public boolean canTransitionTo(TicketStatus next) {
        return switch (this) {
            case NUEVO -> next == EVALUANDO;
            case EVALUANDO -> next == ASIGNADO || next == REVISION_REQUERIDA
                    || next == INFORMACION_INSUFICIENTE;
            // Cuando el cliente responde se vuelve a evaluar; si nunca responde, se cierra.
            case INFORMACION_INSUFICIENTE -> next == EVALUANDO || next == CERRADO;
            case REVISION_REQUERIDA -> next == ASIGNADO || next == CERRADO;
            case ASIGNADO -> next == EN_PROGRESO;
            case EN_PROGRESO -> next == RESUELTO;
            case RESUELTO -> next == CERRADO;
            case CERRADO -> false;
        };
    }
}
