package com.franco.opsengine.outbox.adapter.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Limit limit);

    long countByPublishedAtIsNull();
}
