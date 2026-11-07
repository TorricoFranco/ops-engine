package com.franco.opsengine.audit.application;

import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.ticket.domain.TicketId;

import java.util.List;
import java.util.Optional;

public interface DecisionRecordRepository {

    void record(DecisionRecord decision);

    /** Historial del ticket, de la más reciente a la más vieja. */
    List<DecisionRecord> findByTicket(TicketId ticketId);

    Optional<DecisionRecord> findLatestByTicket(TicketId ticketId);

    long countAll();

    long countByOutcome(DecisionRecord.Outcome outcome);
}
