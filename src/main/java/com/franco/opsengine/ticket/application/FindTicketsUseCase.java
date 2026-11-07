package com.franco.opsengine.ticket.application;

import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindTicketsUseCase {

    private final TicketRepository tickets;

    public FindTicketsUseCase(TicketRepository tickets) {
        this.tickets = tickets;
    }

    public Ticket byId(TicketId id) {
        return tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public List<Ticket> all() {
        return tickets.findAll();
    }
}
