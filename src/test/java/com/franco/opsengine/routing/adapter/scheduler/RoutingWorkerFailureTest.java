package com.franco.opsengine.routing.adapter.scheduler;

import com.franco.opsengine.routing.application.RouteTicketUseCase;
import com.franco.opsengine.routing.application.RoutingMetrics;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Impact;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Qué hace el worker cuando el ruteo de un ticket falla. Sin base ni Spring: todo es un mock. */
class RoutingWorkerFailureTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Skill BILLING = new Skill("BILLING");

    private final TicketRepository tickets = mock(TicketRepository.class);
    private final RouteTicketUseCase routeTicket = mock(RouteTicketUseCase.class);
    private final RoutingMetrics metrics = mock(RoutingMetrics.class);

    private static Ticket waitingTicket() {
        return Ticket.open(TicketId.newId(), "Cobros duplicados", "Nos llegaron dos facturas",
                Impact.HIGH, BILLING, NOW, NOW.plus(Duration.ofHours(4)));
    }

    @Test
    void aFailureIsRecordedAgainstTheTicketWithTheConfiguredLimit() {
        Ticket ticket = waitingTicket();
        when(tickets.findByStatus(TicketStatus.NUEVO, 25)).thenReturn(List.of(ticket));
        when(routeTicket.route(ticket.id())).thenThrow(new IllegalStateException("boom"));
        RoutingWorker worker = new RoutingWorker(tickets, routeTicket, metrics, 25, 3);

        worker.routePending();

        verify(routeTicket).recordFailedAttempt(ticket.id(), "IllegalStateException", 3);
    }

    @Test
    void aFailedRecordingDoesNotStopTheRestOfTheBatch() {
        Ticket failing = waitingTicket();
        Ticket healthy = waitingTicket();
        when(tickets.findByStatus(TicketStatus.NUEVO, 25)).thenReturn(List.of(failing, healthy));
        when(routeTicket.route(failing.id())).thenThrow(new IllegalStateException("boom"));
        doThrow(new IllegalStateException("la base no responde"))
                .when(routeTicket).recordFailedAttempt(any(), any(), anyInt());
        RoutingWorker worker = new RoutingWorker(tickets, routeTicket, metrics, 25, 3);

        worker.routePending();

        verify(routeTicket).route(healthy.id());
    }

    @Test
    void aSuccessfulRoutingRecordsNoFailure() {
        Ticket ticket = waitingTicket();
        when(tickets.findByStatus(TicketStatus.NUEVO, 25)).thenReturn(List.of(ticket));
        RoutingWorker worker = new RoutingWorker(tickets, routeTicket, metrics, 25, 3);

        worker.routePending();

        verify(routeTicket, never()).recordFailedAttempt(any(), any(), anyInt());
    }
}
