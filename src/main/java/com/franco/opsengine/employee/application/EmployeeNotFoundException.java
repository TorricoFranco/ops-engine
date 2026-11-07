package com.franco.opsengine.employee.application;

import com.franco.opsengine.shared.domain.EmployeeId;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(EmployeeId id) {
        super("employee not found: " + id.value());
    }
}
