package com.franco.opsengine.ticket.adapter.web;

import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.ticket.application.CreateTicketCommand;
import com.franco.opsengine.ticket.application.CreateTicketUseCase;
import com.franco.opsengine.ticket.application.FindTicketsUseCase;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets")
class TicketController {

    private final CreateTicketUseCase createTicket;
    private final FindTicketsUseCase findTickets;

    TicketController(CreateTicketUseCase createTicket, FindTicketsUseCase findTickets) {
        this.createTicket = createTicket;
        this.findTickets = findTickets;
    }

    @PostMapping
    @Operation(summary = "Crea un ticket",
            description = "Con el header Idempotency-Key, repetir el pedido devuelve el mismo ticket.")
    ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Ticket ticket = createTicket.execute(new CreateTicketCommand(
                request.title(),
                request.description(),
                request.impact(),
                new Skill(request.requiredSkill()),
                Duration.ofMinutes(request.slaMinutes()),
                idempotencyKey));

        return ResponseEntity
                .created(URI.create("/api/tickets/" + ticket.id().value()))
                .body(TicketResponse.from(ticket));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Devuelve un ticket por id")
    TicketResponse byId(@PathVariable UUID id) {
        return TicketResponse.from(findTickets.byId(new TicketId(id)));
    }

    @GetMapping
    @Operation(summary = "Lista los tickets")
    List<TicketResponse> all() {
        return findTickets.all().stream().map(TicketResponse::from).toList();
    }
}
