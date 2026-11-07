package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;

final class TicketMapper {

    private TicketMapper() {
    }

    static TicketEntity toEntity(Ticket ticket, String idempotencyKey) {
        return new TicketEntity(
                ticket.id().value(),
                ticket.title(),
                ticket.description(),
                ticket.impact(),
                ticket.requiredSkill().code(),
                ticket.status(),
                ticket.assignee().map(EmployeeId::value).orElse(null),
                ticket.reviewReason().orElse(null),
                ticket.createdAt(),
                ticket.slaDeadline(),
                idempotencyKey);
    }

    static Ticket toDomain(TicketEntity entity) {
        return Ticket.rebuild(
                new TicketId(entity.getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getImpact(),
                new Skill(entity.getRequiredSkill()),
                entity.getCreatedAt(),
                entity.getSlaDeadline(),
                entity.getStatus(),
                entity.getAssigneeId() == null ? null : new EmployeeId(entity.getAssigneeId()),
                entity.getReviewReason());
    }
}
