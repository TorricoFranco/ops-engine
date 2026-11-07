package com.franco.opsengine.audit.adapter.persistence;

import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.routing.domain.ReviewReason;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decision_records")
public class DecisionRecordEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "suggested_skill", length = 50)
    private String suggestedSkill;

    @Column(name = "skill_confidence")
    private Double skillConfidence;

    @Column(name = "skill_recognized")
    private Double skillRecognized;

    private Double urgency;

    @Column(name = "urgency_levels")
    private Integer urgencyLevels;

    @Column(name = "multiple_customers")
    private Double multipleCustomers;

    @Column(name = "service_unavailable")
    private Double serviceUnavailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecisionRecord.Outcome outcome;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_reason", length = 50)
    private ReviewReason reviewReason;

    @Column(nullable = false, columnDefinition = "text")
    private String rationale;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @OneToMany(mappedBy = "decision", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<DecisionCandidateEntity> candidates = new ArrayList<>();

    protected DecisionRecordEntity() {
        // requerido por JPA
    }

    DecisionRecordEntity(UUID id, UUID ticketId, String provider, String policyVersion,
                         String suggestedSkill, Double skillConfidence, Double skillRecognized,
                         Double urgency, Integer urgencyLevels, Double multipleCustomers,
                         Double serviceUnavailable, DecisionRecord.Outcome outcome,
                         UUID assignedEmployeeId, ReviewReason reviewReason, String rationale,
                         long latencyMs, Instant decidedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.provider = provider;
        this.policyVersion = policyVersion;
        this.suggestedSkill = suggestedSkill;
        this.skillConfidence = skillConfidence;
        this.skillRecognized = skillRecognized;
        this.urgency = urgency;
        this.urgencyLevels = urgencyLevels;
        this.multipleCustomers = multipleCustomers;
        this.serviceUnavailable = serviceUnavailable;
        this.outcome = outcome;
        this.assignedEmployeeId = assignedEmployeeId;
        this.reviewReason = reviewReason;
        this.rationale = rationale;
        this.latencyMs = latencyMs;
        this.decidedAt = decidedAt;
    }

    void addCandidate(DecisionCandidateEntity candidate) {
        candidate.setDecision(this);
        candidates.add(candidate);
    }

    UUID getId() {
        return id;
    }

    UUID getTicketId() {
        return ticketId;
    }

    String getProvider() {
        return provider;
    }

    String getPolicyVersion() {
        return policyVersion;
    }

    String getSuggestedSkill() {
        return suggestedSkill;
    }

    Double getSkillConfidence() {
        return skillConfidence;
    }

    Double getSkillRecognized() {
        return skillRecognized;
    }

    Double getUrgency() {
        return urgency;
    }

    Integer getUrgencyLevels() {
        return urgencyLevels;
    }

    Double getMultipleCustomers() {
        return multipleCustomers;
    }

    Double getServiceUnavailable() {
        return serviceUnavailable;
    }

    DecisionRecord.Outcome getOutcome() {
        return outcome;
    }

    UUID getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    ReviewReason getReviewReason() {
        return reviewReason;
    }

    String getRationale() {
        return rationale;
    }

    long getLatencyMs() {
        return latencyMs;
    }

    Instant getDecidedAt() {
        return decidedAt;
    }

    List<DecisionCandidateEntity> getCandidates() {
        return candidates;
    }
}
