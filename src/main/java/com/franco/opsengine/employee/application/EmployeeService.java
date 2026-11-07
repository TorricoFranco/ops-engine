package com.franco.opsengine.employee.application;

import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * RF-11: alta y consulta de empleados. El ruteo que los usa llega en la fase 4.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employees;

    public EmployeeService(EmployeeRepository employees) {
        this.employees = employees;
    }

    public Employee register(String name, TeamId teamId, Set<Skill> skills, int maxOpenTickets) {
        return employees.save(Employee.create(EmployeeId.newId(), name, teamId, skills, maxOpenTickets));
    }

    public Employee byId(EmployeeId id) {
        return employees.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public List<Employee> all() {
        return employees.findAll();
    }

    public List<Employee> withSkill(Skill skill) {
        return employees.findBySkill(skill);
    }
}
