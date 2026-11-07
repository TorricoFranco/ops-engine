package com.franco.opsengine.shared.domain;

import java.util.Objects;
import java.util.UUID;

public record TeamId(UUID value) {

    public TeamId {
        Objects.requireNonNull(value, "team id is required");
    }

    public static TeamId newId() {
        return new TeamId(UUID.randomUUID());
    }
}
