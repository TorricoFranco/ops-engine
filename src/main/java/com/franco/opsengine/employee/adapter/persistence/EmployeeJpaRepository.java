package com.franco.opsengine.employee.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {

    @Query("select e from EmployeeEntity e join e.skills s where s = :skillCode")
    List<EmployeeEntity> findBySkillCode(@Param("skillCode") String skillCode);
}
