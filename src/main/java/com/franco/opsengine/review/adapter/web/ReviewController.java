package com.franco.opsengine.review.adapter.web;

import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.review.application.ResolveReviewUseCase;
import com.franco.opsengine.review.application.ReviewInbox;
import com.franco.opsengine.review.application.ReviewMetrics;
import com.franco.opsengine.review.domain.ReviewResolution;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/review")
@Tag(name = "Revisión humana")
class ReviewController {

    private final ReviewInbox inbox;
    private final ResolveReviewUseCase resolveReview;
    private final ReviewMetrics metrics;

    ReviewController(ReviewInbox inbox, ResolveReviewUseCase resolveReview, ReviewMetrics metrics) {
        this.inbox = inbox;
        this.resolveReview = resolveReview;
        this.metrics = metrics;
    }

    record PendingView(
            UUID ticketId,
            String title,
            TicketStatus status,
            Instant slaDeadline,
            ReviewReason reviewReason,
            String rationale,
            String suggestedSkill,
            Double skillConfidence) {

        static PendingView from(ReviewInbox.PendingReview pending) {
            Ticket ticket = pending.ticket();
            DecisionRecord decision = pending.decision();
            return new PendingView(
                    ticket.id().value(),
                    ticket.title(),
                    ticket.status(),
                    ticket.slaDeadline(),
                    decision == null ? null : decision.review().orElse(null),
                    decision == null ? null : decision.rationale(),
                    decision == null ? null : decision.judgementSnapshot()
                            .map(j -> j.suggestedSkill()).orElse(null),
                    decision == null ? null : decision.judgementSnapshot()
                            .map(j -> j.skillConfidence()).orElse(null));
        }
    }

    /**
     * @param agreedWithSuggestion si el supervisor coincidió con lo que sugirió el sistema.
     *                             Cuando es false, el motivo es obligatorio.
     */
    record AssignRequest(
            @NotNull UUID employeeId,
            Boolean agreedWithSuggestion,
            String disagreementReason) {
    }

    record CloseRequest(Boolean agreedWithSuggestion, String disagreementReason) {
    }

    record ResolutionView(
            UUID id,
            UUID ticketId,
            String resolution,
            UUID assignedEmployeeId,
            Boolean agreedWithSuggestion,
            String disagreementReason,
            boolean override,
            String resolvedBy,
            Instant resolvedAt) {

        static ResolutionView from(ReviewResolution resolution) {
            return new ResolutionView(
                    resolution.id(),
                    resolution.ticketId().value(),
                    resolution.resolution().name(),
                    resolution.assignee().map(EmployeeId::value).orElse(null),
                    resolution.agreedWithSuggestion(),
                    resolution.disagreementReason(),
                    resolution.isOverride(),
                    resolution.resolvedBy(),
                    resolution.resolvedAt());
        }
    }

    @GetMapping
    @Operation(summary = "Tickets esperando a una persona, el más urgente primero")
    List<PendingView> pending() {
        return inbox.pending().stream().map(PendingView::from).toList();
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Asigna un ticket en revisión y registra si hubo desacuerdo")
    ResolutionView assign(@PathVariable UUID ticketId,
                          @Valid @RequestBody AssignRequest request,
                          @AuthenticationPrincipal Jwt principal) {
        return ResolutionView.from(resolveReview.assign(
                new TicketId(ticketId),
                new EmployeeId(request.employeeId()),
                principal.getSubject(),
                request.agreedWithSuggestion(),
                request.disagreementReason()));
    }

    @PostMapping("/{ticketId}/close")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Cierra un ticket en revisión sin asignarlo")
    ResolutionView close(@PathVariable UUID ticketId,
                         @Valid @RequestBody CloseRequest request,
                         @AuthenticationPrincipal Jwt principal) {
        return ResolutionView.from(resolveReview.close(
                new TicketId(ticketId),
                principal.getSubject(),
                request.agreedWithSuggestion(),
                request.disagreementReason()));
    }

    /**
     * Es el mismo contenido que {@link ReviewMetrics.Snapshot}, pero declarado acá.
     *
     * <p>Devolver el record de la capa de aplicación publicaba su nombre interno en el
     * contrato: el OpenAPI traía un esquema llamado "Snapshot", y renombrar ese record
     * habría cambiado la API sin que nadie lo notara.
     *
     * @param reviewRate   proporción que necesitó una persona
     * @param overrideRate overrides sobre comparables. Nulo mientras no haya comparables: un
     *                     cero inventado se leería como que el motor nunca se equivoca
     */
    record MetricsView(
            long decisions,
            long assigned,
            long sentToReview,
            Double reviewRate,
            long resolutions,
            long comparable,
            long overrides,
            Double overrideRate) {

        static MetricsView from(ReviewMetrics.Snapshot snapshot) {
            return new MetricsView(
                    snapshot.decisions(), snapshot.assigned(), snapshot.sentToReview(),
                    snapshot.reviewRate(), snapshot.resolutions(), snapshot.comparable(),
                    snapshot.overrides(), snapshot.overrideRate());
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Tasa de revisión y de desacuerdo con el sistema")
    MetricsView metrics() {
        return MetricsView.from(metrics.snapshot());
    }
}
