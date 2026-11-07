package com.franco.opsengine.decision.application;

import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;

/**
 * Puerto hacia el proveedor de juicios. El dominio no sabe si detrás hay un modelo, una
 * heurística o un stub, y cambiar de proveedor no toca las reglas de negocio (ADR-005).
 */
public interface DecisionProvider {

    /**
     * @throws DecisionUnavailableException si no se pudo obtener un juicio. Nunca devuelve un
     *                                      juicio inventado para disimular una falla.
     */
    TicketJudgement judge(TicketContext context);

    /** Identifica al proveedor en la auditoría de decisiones (fase 5). */
    String name();
}
