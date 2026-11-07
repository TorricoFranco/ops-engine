package com.franco.opsengine.ticket.domain;

import java.util.Objects;
import java.util.UUID;

public record TicketId(UUID value) {

    public TicketId {
        Objects.requireNonNull(value, "ticket id is required");
    }

    public static TicketId newId() {
        return new TicketId(UUID.randomUUID());
    }
}
