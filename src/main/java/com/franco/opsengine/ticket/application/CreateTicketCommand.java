package com.franco.opsengine.ticket.application;

import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.ticket.domain.Impact;

import java.time.Duration;

/**
 * @param idempotencyKey clave opcional. Dos pedidos con la misma clave devuelven el mismo ticket.
 */
public record CreateTicketCommand(
        String title,
        String description,
        Impact impact,
        Skill requiredSkill,
        Duration slaDuration,
        String idempotencyKey) {
}
