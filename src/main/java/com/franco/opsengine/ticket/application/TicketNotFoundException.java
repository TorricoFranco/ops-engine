package com.franco.opsengine.ticket.application;

import com.franco.opsengine.ticket.domain.TicketId;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(TicketId id) {
        super("ticket not found: " + id.value());
    }
}
