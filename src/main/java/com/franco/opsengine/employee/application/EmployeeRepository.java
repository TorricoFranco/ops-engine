package com.franco.opsengine.employee.application;

import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findById(EmployeeId id);

    List<Employee> findAll();

    /** Candidatos con la skill pedida. El filtrado fino por carga y permisos llega en la fase 4. */
    List<Employee> findBySkill(Skill skill);
}
