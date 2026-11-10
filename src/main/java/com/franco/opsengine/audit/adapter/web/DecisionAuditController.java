package com.franco.opsengine.audit.adapter.web;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.audit.domain.JudgementSnapshot;
import com.franco.opsengine.routing.domain.CandidateEvaluation;
import com.franco.opsengine.routing.domain.RejectionReason;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/decisions")
@Tag(name = "Auditoría de decisiones")
class DecisionAuditController {

    private final DecisionRecordRepository decisions;

    DecisionAuditController(DecisionRecordRepository decisions) {
        this.decisions = decisions;
    }

    record JudgementView(
            String suggestedSkill,
            double skillConfidence,
            double skillRecognized,
            double urgency,
            int urgencyLevels,
            double multipleCustomersAffected,
            double serviceUnavailable) {

        static JudgementView from(JudgementSnapshot snapshot) {
            return new JudgementView(
                    snapshot.suggestedSkill(), snapshot.skillConfidence(), snapshot.skillRecognized(),
                    snapshot.urgency(), snapshot.urgencyLevels(),
                    snapshot.multipleCustomersAffected(), snapshot.serviceUnavailable());
        }
    }

    /**
     * El motivo de descarte viaja como enum y no como String.
     *
     * <p>Jackson lo serializa con el mismo texto, así que el JSON no cambia, pero el OpenAPI
     * pasa a declarar los valores posibles en lugar de "una cadena cualquiera". De eso
     * dependen los tipos del frontend: con String, el generador produce `string` y agregar un
     * motivo nuevo no rompe nada, solo deja la pantalla mostrando el enum crudo.
     *
     * <p>El nombre del esquema es explícito porque el ruteo tiene otro record con el mismo
     * nombre simple, y springdoc indexa por nombre simple.
     */
    @Schema(name = "DecisionCandidateView")
    record CandidateView(
            UUID employeeId, String name, boolean eligible, RejectionReason rejectionReason,
            int openTickets, int maxOpenTickets) {

        static CandidateView from(CandidateEvaluation evaluation) {
            return new CandidateView(
                    evaluation.employeeId().value(), evaluation.employeeName(), evaluation.eligible(),
                    evaluation.rejection().orElse(null),
                    evaluation.openTickets(), evaluation.maxOpenTickets());
        }
    }

    record DecisionView(
            UUID id,
            String provider,
            String policyVersion,
            DecisionRecord.Outcome outcome,
            UUID assignedEmployeeId,
            ReviewReason reviewReason,
            String rationale,
            JudgementView judgement,
            List<CandidateView> candidates,
            long latencyMs,
            Instant decidedAt) {

        static DecisionView from(DecisionRecord record) {
            return new DecisionView(
                    record.id(),
                    record.provider(),
                    record.policyVersion(),
                    record.outcome(),
                    record.assignee().map(EmployeeId::value).orElse(null),
                    record.review().orElse(null),
                    record.rationale(),
                    record.judgementSnapshot().map(JudgementView::from).orElse(null),
                    record.candidates().stream().map(CandidateView::from).toList(),
                    record.latencyMs(),
                    record.decidedAt());
        }
    }

    @GetMapping
    @Operation(summary = "Historial de decisiones del ticket, de la más reciente a la más vieja")
    List<DecisionView> forTicket(@PathVariable UUID ticketId) {
        return decisions.findByTicket(new TicketId(ticketId)).stream()
                .map(DecisionView::from)
                .toList();
    }
}
