package com.franco.opsengine.routing.adapter.web;

import com.franco.opsengine.routing.application.RouteTicketUseCase;
import com.franco.opsengine.routing.domain.CandidateEvaluation;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.ticket.domain.TicketId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{id}/routing")
@Tag(name = "Ruteo")
class RoutingController {

    private final RouteTicketUseCase routeTicket;

    RoutingController(RouteTicketUseCase routeTicket) {
        this.routeTicket = routeTicket;
    }

    /**
     * La respuesta trae la decisión y por qué: qué candidatos se evaluaron, cuáles quedaron
     * afuera y por qué motivo, y con qué versión de reglas se decidió.
     */
    record RoutingResponse(
            String outcome,
            UUID assigneeId,
            String reviewReason,
            String rationale,
            String policyVersion,
            List<CandidateView> candidates) {

        static RoutingResponse from(RoutingOutcome outcome) {
            List<CandidateView> candidates = outcome.candidates().stream().map(CandidateView::from).toList();
            return switch (outcome) {
                case RoutingOutcome.Assign assign -> new RoutingResponse(
                        "ASSIGNED", assign.employeeId().value(), null,
                        assign.rationale(), assign.policyVersion(), candidates);
                case RoutingOutcome.SendToReview review -> new RoutingResponse(
                        "SENT_TO_REVIEW", null, review.reason().name(),
                        review.rationale(), review.policyVersion(), candidates);
                case RoutingOutcome.RequestMoreInformation more -> new RoutingResponse(
                        "NEEDS_MORE_INFORMATION", null, null,
                        more.rationale(), more.policyVersion(), candidates);
            };
        }
    }

    record CandidateView(
            UUID employeeId,
            String name,
            boolean eligible,
            String rejectionReason,
            int openTickets,
            int maxOpenTickets) {

        static CandidateView from(CandidateEvaluation evaluation) {
            return new CandidateView(
                    evaluation.employeeId().value(),
                    evaluation.employeeName(),
                    evaluation.eligible(),
                    evaluation.rejection().map(Enum::name).orElse(null),
                    evaluation.openTickets(),
                    evaluation.maxOpenTickets());
        }
    }

    @PostMapping
    @Operation(summary = "Rutea un ticket y devuelve la decisión con su explicación")
    ResponseEntity<RoutingResponse> route(@PathVariable UUID id) {
        RoutingOutcome outcome = routeTicket.route(new TicketId(id));
        return ResponseEntity.ok(RoutingResponse.from(outcome));
    }
}
