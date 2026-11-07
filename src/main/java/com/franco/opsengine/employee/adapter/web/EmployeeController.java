package com.franco.opsengine.employee.adapter.web;

import com.franco.opsengine.employee.adapter.web.EmployeeDtos.EmployeeResponse;
import com.franco.opsengine.employee.adapter.web.EmployeeDtos.RegisterEmployeeRequest;
import com.franco.opsengine.employee.application.EmployeeService;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Empleados")
class EmployeeController {

    private final EmployeeService employees;

    EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra un empleado (solo ADMIN)")
    ResponseEntity<EmployeeResponse> register(@Valid @RequestBody RegisterEmployeeRequest request) {
        Set<Skill> skills = request.skills().stream().map(Skill::new).collect(Collectors.toSet());
        var employee = employees.register(
                request.name(), new TeamId(request.teamId()), skills, request.maxOpenTickets(),
                request.clearanceOrDefault());

        return ResponseEntity
                .created(URI.create("/api/employees/" + employee.id().value()))
                .body(EmployeeResponse.from(employee));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Devuelve un empleado por id")
    EmployeeResponse byId(@PathVariable UUID id) {
        return EmployeeResponse.from(employees.byId(new EmployeeId(id)));
    }

    @GetMapping
    @Operation(summary = "Lista empleados, opcionalmente filtrados por skill")
    List<EmployeeResponse> all(@RequestParam(required = false) String skill) {
        var found = skill == null ? employees.all() : employees.withSkill(new Skill(skill));
        return found.stream().map(EmployeeResponse::from).toList();
    }
}
