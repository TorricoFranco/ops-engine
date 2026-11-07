package com.franco.opsengine.ticket.domain;

public class InvalidTicketTransitionException extends RuntimeException {

    private final TicketStatus from;
    private final TicketStatus to;

    public InvalidTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("cannot move ticket from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public TicketStatus from() {
        return from;
    }

    public TicketStatus to() {
        return to;
    }
}
