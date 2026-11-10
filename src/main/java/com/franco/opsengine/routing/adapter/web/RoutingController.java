package com.franco.opsengine.routing.adapter.web;

import com.franco.opsengine.routing.application.RouteTicketUseCase;
import com.franco.opsengine.routing.domain.CandidateEvaluation;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.routing.domain.RejectionReason;
import com.franco.opsengine.routing.domain.ReviewReason;
import io.swagger.v3.oas.annotations.media.Schema;
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
            DecisionRecord.Outcome outcome,
            UUID assigneeId,
            ReviewReason reviewReason,
            String rationale,
            String policyVersion,
            List<CandidateView> candidates) {

        static RoutingResponse from(RoutingOutcome outcome) {
            List<CandidateView> candidates = outcome.candidates().stream().map(CandidateView::from).toList();
            return switch (outcome) {
                case RoutingOutcome.Assign assign -> new RoutingResponse(
                        DecisionRecord.Outcome.ASSIGNED, assign.employeeId().value(), null,
                        assign.rationale(), assign.policyVersion(), candidates);
                case RoutingOutcome.SendToReview review -> new RoutingResponse(
                        DecisionRecord.Outcome.SENT_TO_REVIEW, null, review.reason(),
                        review.rationale(), review.policyVersion(), candidates);
                case RoutingOutcome.RequestMoreInformation more -> new RoutingResponse(
                        DecisionRecord.Outcome.NEEDS_MORE_INFORMATION, null, null,
                        more.rationale(), more.policyVersion(), candidates);
            };
        }
    }

    /**
     * El nombre del esquema es explícito porque la auditoría tiene otro record con el mismo
     * nombre simple. springdoc indexa por nombre simple: sin esto, uno pisa al otro y el
     * OpenAPI documenta uno de los dos endpoints con la forma del otro.
     */
    @Schema(name = "RoutingCandidateView")
    record CandidateView(
            UUID employeeId,
            String name,
            boolean eligible,
            RejectionReason rejectionReason,
            int openTickets,
            int maxOpenTickets) {

        static CandidateView from(CandidateEvaluation evaluation) {
            return new CandidateView(
                    evaluation.employeeId().value(),
                    evaluation.employeeName(),
                    evaluation.eligible(),
                    evaluation.rejection().orElse(null),
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
