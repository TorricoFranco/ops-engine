package com.franco.opsengine.ticket.adapter.web;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.Impact;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        Impact impact,
        String requiredSkill,
        TicketStatus status,
        UUID assigneeId,
        String reviewReason,
        Instant createdAt,
        Instant slaDeadline) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.id().value(),
                ticket.title(),
                ticket.description(),
                ticket.impact(),
                ticket.requiredSkill().code(),
                ticket.status(),
                ticket.assignee().map(EmployeeId::value).orElse(null),
                ticket.reviewReason().orElse(null),
                ticket.createdAt(),
                ticket.slaDeadline());
    }
}
