package com.franco.opsengine.config;

import com.franco.opsengine.routing.application.RouteTicketUseCase;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Impact;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Carga tickets de ejemplo y los rutea, solo con el perfil {@code dev}.
 *
 * <p>Existe para que el panel tenga algo que mostrar apenas se levanta. Sin esto la primera
 * impresión es una pantalla vacía, y hay que crear tickets a mano para ver cómo funciona.
 *
 * <p>Los casos están elegidos para que se vea el abanico completo: asignaciones claras, un
 * SLA por vencer que vuelve crítico el ticket, uno que no corresponde a ninguna área y uno
 * ilegible. No se ejecuta si ya hay tickets cargados.
 */
@Configuration
@Profile("dev")
class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** @param slaMinutes minutos hasta el vencimiento; con pocos, el caso se vuelve crítico */
    private record DemoTicket(String title, String description, Impact impact, String skill,
                              int slaMinutes) {
    }

    private static final List<DemoTicket> DEMO_TICKETS = List.of(
            new DemoTicket("Cobros duplicados",
                    "Nos llegaron dos facturas por el mismo mes y el importe se debitó dos veces",
                    Impact.HIGH, "BILLING", 480),
            new DemoTicket("No puedo entrar al panel",
                    "Desde ayer me rechaza la contraseña aunque la reseteé dos veces",
                    Impact.MEDIUM, "ACCESS_IDENTITY", 360),
            new DemoTicket("El webhook dejó de dispararse",
                    "Los pedidos nuevos no llegan desde la última actualización de la integración",
                    Impact.HIGH, "INTEGRATIONS", 240),
            new DemoTicket("El panel devuelve error 500",
                    "Hace veinte minutos que todo el equipo recibe errores del servidor",
                    Impact.CRITICAL, "INFRASTRUCTURE", 25),
            new DemoTicket("Quiero pasar al plan empresarial",
                    "Necesito saber qué incluye el plan grande y cómo hago el cambio",
                    Impact.LOW, "ACCOUNT_MANAGEMENT", 2880),
            new DemoTicket("Me cobraron de más",
                    "El plan dice cien pero el débito salió ciento cincuenta",
                    Impact.MEDIUM, "BILLING", 600),
            new DemoTicket("La sincronización viene desfasada",
                    "Los productos que cargamos tardan horas en aparecer del otro lado",
                    Impact.MEDIUM, "INTEGRATIONS", 420),
            new DemoTicket("Un usuario no ve los reportes",
                    "A una persona del equipo le falta el permiso para abrir los reportes mensuales",
                    Impact.LOW, "ACCESS_IDENTITY", 720),
            new DemoTicket("asd",
                    "sadnsdns",
                    Impact.LOW, "BILLING", 480),
            new DemoTicket("Consulta",
                    "Quería saber hasta qué hora atienden los sábados",
                    Impact.LOW, "BILLING", 1440));

    @Bean
    ApplicationRunner seedDemoTickets(TicketRepository tickets, RouteTicketUseCase routeTicket,
                                      Clock clock) {
        return args -> {
            if (!tickets.findAll().isEmpty()) {
                log.info("Demo tickets already present, skipping");
                return;
            }

            Instant now = clock.instant();
            for (DemoTicket demo : DEMO_TICKETS) {
                Ticket ticket = tickets.save(Ticket.open(
                        TicketId.newId(), demo.title(), demo.description(), demo.impact(),
                        new Skill(demo.skill()), now,
                        now.plus(Duration.ofMinutes(demo.slaMinutes()))), null);
                routeTicket.route(ticket.id());
            }
            log.info("Demo tickets created and routed count={}", DEMO_TICKETS.size());
        };
    }
}
