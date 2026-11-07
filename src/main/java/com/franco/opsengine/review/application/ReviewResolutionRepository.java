package com.franco.opsengine.review.application;

import com.franco.opsengine.review.domain.ReviewResolution;
import com.franco.opsengine.ticket.domain.TicketId;

import java.util.List;

public interface ReviewResolutionRepository {

    void record(ReviewResolution resolution);

    List<ReviewResolution> findByTicket(TicketId ticketId);

    long countAll();

    /** Cuántas veces la persona contradijo la sugerencia del sistema. */
    long countOverrides();

    /** Resoluciones donde hubo una sugerencia que contrastar. Es el divisor del override. */
    long countWithSuggestion();
}
