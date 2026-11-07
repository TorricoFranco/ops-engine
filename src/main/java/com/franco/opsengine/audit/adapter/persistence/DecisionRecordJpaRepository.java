package com.franco.opsengine.audit.adapter.persistence;

import com.franco.opsengine.audit.domain.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DecisionRecordJpaRepository extends JpaRepository<DecisionRecordEntity, UUID> {

    List<DecisionRecordEntity> findByTicketIdOrderByDecidedAtDesc(UUID ticketId);

    long countByOutcome(DecisionRecord.Outcome outcome);
}
