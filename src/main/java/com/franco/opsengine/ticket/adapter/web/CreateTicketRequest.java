package com.franco.opsengine.ticket.adapter.web;

import com.franco.opsengine.ticket.domain.Impact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull Impact impact,
        @NotBlank String requiredSkill,
        @NotNull @Positive Integer slaMinutes) {
}
