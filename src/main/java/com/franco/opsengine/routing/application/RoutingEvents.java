package com.franco.opsengine.routing.application;

/** Tipos de evento que publica el ruteo. */
public final class RoutingEvents {

    public static final String TICKET_ASSIGNED = "ticket.assigned";
    public static final String TICKET_SENT_TO_REVIEW = "ticket.sent-to-review";

    private RoutingEvents() {
    }
}
