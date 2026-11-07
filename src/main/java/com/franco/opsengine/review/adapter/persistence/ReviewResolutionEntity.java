package com.franco.opsengine.review.adapter.persistence;

import com.franco.opsengine.review.domain.ReviewResolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_resolutions")
public class ReviewResolutionEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "decision_record_id")
    private UUID decisionRecordId;

    @Column(name = "resolved_by", nullable = false, length = 100)
    private String resolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewResolution.Resolution resolution;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "agreed_with_suggestion")
    private Boolean agreedWithSuggestion;

    @Column(name = "disagreement_reason", columnDefinition = "text")
    private String disagreementReason;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    protected ReviewResolutionEntity() {
        // requerido por JPA
    }

    ReviewResolutionEntity(UUID id, UUID ticketId, UUID decisionRecordId, String resolvedBy,
                           ReviewResolution.Resolution resolution, UUID assignedEmployeeId,
                           Boolean agreedWithSuggestion, String disagreementReason, Instant resolvedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.decisionRecordId = decisionRecordId;
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
        this.assignedEmployeeId = assignedEmployeeId;
        this.agreedWithSuggestion = agreedWithSuggestion;
        this.disagreementReason = disagreementReason;
        this.resolvedAt = resolvedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getTicketId() {
        return ticketId;
    }

    UUID getDecisionRecordId() {
        return decisionRecordId;
    }

    String getResolvedBy() {
        return resolvedBy;
    }

    ReviewResolution.Resolution getResolution() {
        return resolution;
    }

    UUID getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    Boolean getAgreedWithSuggestion() {
        return agreedWithSuggestion;
    }

    String getDisagreementReason() {
        return disagreementReason;
    }

    Instant getResolvedAt() {
        return resolvedAt;
    }
}
