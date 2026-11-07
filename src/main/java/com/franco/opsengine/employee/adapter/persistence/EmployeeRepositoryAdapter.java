package com.franco.opsengine.employee.adapter.persistence;

import com.franco.opsengine.employee.application.EmployeeRepository;
import com.franco.opsengine.employee.domain.Clearance;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository jpa;

    EmployeeRepositoryAdapter(EmployeeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Employee save(Employee employee) {
        Set<String> skillCodes = employee.skills().stream().map(Skill::code).collect(Collectors.toSet());
        EmployeeEntity entity = new EmployeeEntity(
                employee.id().value(),
                employee.name(),
                employee.teamId().value(),
                employee.availability(),
                employee.maxOpenTickets(),
                employee.openTickets(),
                skillCodes,
                employee.clearance(),
                employee.version());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
        return jpa.findById(id.value()).map(EmployeeRepositoryAdapter::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return jpa.findAll().stream().map(EmployeeRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Employee> findBySkill(Skill skill) {
        return jpa.findBySkillCode(skill.code()).stream().map(EmployeeRepositoryAdapter::toDomain).toList();
    }

    private static Employee toDomain(EmployeeEntity entity) {
        Set<Skill> skills = entity.getSkills().stream().map(Skill::new).collect(Collectors.toSet());
        return Employee.rebuild(
                new EmployeeId(entity.getId()),
                entity.getName(),
                new TeamId(entity.getTeamId()),
                skills,
                entity.getMaxOpenTickets(),
                entity.getClearance(),
                entity.getAvailability(),
                entity.getOpenTickets(),
                entity.getVersion());
    }
}
