package com.franco.opsengine.routing.adapter.scheduler;

import com.franco.opsengine.routing.application.RouteTicketUseCase;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rutea en segundo plano los tickets que están esperando.
 *
 * <p>Crear un ticket responde al instante y el ruteo pasa después. Antes, quien creaba el
 * ticket esperaba a que respondiera el proveedor de juicios: entre 350 ms y el timeout, con
 * una conexión de base tomada todo ese rato. Con miles de tickets por día eso es un cuello de
 * botella por una espera que al cliente no le aporta nada (ADR-010).
 *
 * <p>Procesa de a lotes y los más viejos primero, así un pico no deja tickets rezagados
 * indefinidamente.
 */
@Component
@ConditionalOnProperty(name = "app.routing.worker.enabled", havingValue = "true", matchIfMissing = true)
public class RoutingWorker {

    private static final Logger log = LoggerFactory.getLogger(RoutingWorker.class);

    private final TicketRepository tickets;
    private final RouteTicketUseCase routeTicket;
    private final int batchSize;

    public RoutingWorker(TicketRepository tickets, RouteTicketUseCase routeTicket,
                         @Value("${app.routing.worker.batch-size:25}") int batchSize) {
        this.tickets = tickets;
        this.routeTicket = routeTicket;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.routing.worker.interval-ms:2000}")
    public void routePending() {
        for (Ticket ticket : tickets.findByStatus(TicketStatus.NUEVO, batchSize)) {
            try {
                routeTicket.route(ticket.id());
            } catch (OptimisticLockingFailureException e) {
                // Otro lo tomó primero. No es un error: el ticket ya quedó resuelto.
                log.debug("Ticket already routed by someone else ticketId={}", ticket.id().value());
            } catch (RuntimeException e) {
                // Queda en NUEVO y se reintenta en la próxima pasada. Si algo lo hace fallar
                // siempre, se va a ver en el log y en la cantidad de pendientes.
                log.error("Routing failed, leaving the ticket pending ticketId={}",
                        ticket.id().value(), e);
            }
        }
    }
}
