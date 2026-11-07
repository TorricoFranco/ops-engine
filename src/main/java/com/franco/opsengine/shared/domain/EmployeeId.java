package com.franco.opsengine.shared.domain;

import java.util.Objects;
import java.util.UUID;

public record EmployeeId(UUID value) {

    public EmployeeId {
        Objects.requireNonNull(value, "employee id is required");
    }

    public static EmployeeId newId() {
        return new EmployeeId(UUID.randomUUID());
    }
}
