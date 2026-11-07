package com.franco.opsengine.employee.adapter.web;

import com.franco.opsengine.employee.domain.Availability;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.shared.domain.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class EmployeeDtos {

    private EmployeeDtos() {
    }

    record RegisterEmployeeRequest(
            @NotBlank String name,
            @NotNull UUID teamId,
            @NotEmpty Set<String> skills,
            @NotNull @Positive Integer maxOpenTickets) {
    }

    record EmployeeResponse(
            UUID id,
            String name,
            UUID teamId,
            List<String> skills,
            int maxOpenTickets,
            int openTickets,
            Availability availability) {

        static EmployeeResponse from(Employee employee) {
            return new EmployeeResponse(
                    employee.id().value(),
                    employee.name(),
                    employee.teamId().value(),
                    employee.skills().stream().map(Skill::code).sorted().toList(),
                    employee.maxOpenTickets(),
                    employee.openTickets(),
                    employee.availability());
        }
    }
}
