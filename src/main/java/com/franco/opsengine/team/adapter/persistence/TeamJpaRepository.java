package com.franco.opsengine.team.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TeamJpaRepository extends JpaRepository<TeamEntity, UUID> {
}
