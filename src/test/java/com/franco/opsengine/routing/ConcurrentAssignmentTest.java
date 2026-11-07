package com.franco.opsengine.routing;

import com.franco.opsengine.employee.application.EmployeeRepository;
import com.franco.opsengine.employee.domain.Clearance;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;
import com.franco.opsengine.support.AbstractIntegrationTest;
import com.franco.opsengine.team.application.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-06: dos asignaciones simultáneas al mismo empleado no se pisan.
 *
 * <p>Sin control de concurrencia las dos leerían la misma carga, las dos sumarían uno y la
 * carga final quedaría en uno en lugar de dos: un empleado por encima de su capacidad sin que
 * nadie se entere. La versión que viaja en el aggregate hace que la segunda escritura falle.
 */
class ConcurrentAssignmentTest extends AbstractIntegrationTest {

    private static final Skill BILLING = new Skill("BILLING");

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private TeamService teams;

    @Test
    void twoSimultaneousAssignmentsToTheSameAgentDoNotOverwriteEachOther() throws Exception {
        clearOperationalData();
        TeamId team = teams.create("Concurrencia " + UUID.randomUUID()).id();
        Employee saved = employees.save(Employee.create(
                EmployeeId.newId(), "Bruno", team, Set.of(BILLING), 5, Clearance.STANDARD));
        EmployeeId employeeId = saved.id();

        // Las dos leen el mismo estado antes de que ninguna escriba.
        Employee firstRead = employees.findById(employeeId).orElseThrow();
        Employee secondRead = employees.findById(employeeId).orElseThrow();
        firstRead.takeTicket(BILLING);
        secondRead.takeTicket(BILLING);

        CyclicBarrier startTogether = new CyclicBarrier(2);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Void> first = pool.submit(saveTask(firstRead, startTogether, succeeded, conflicted));
            Future<Void> second = pool.submit(saveTask(secondRead, startTogether, succeeded, conflicted));
            first.get();
            second.get();
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(conflicted.get()).isEqualTo(1);

        Employee reloaded = employees.findById(employeeId).orElseThrow();
        assertThat(reloaded.openTickets()).isEqualTo(1);
    }

    private Callable<Void> saveTask(Employee employee, CyclicBarrier barrier,
                                    AtomicInteger succeeded, AtomicInteger conflicted) {
        return () -> {
            barrier.await();
            try {
                employees.save(employee);
                succeeded.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflicted.incrementAndGet();
            }
            return null;
        };
    }
}
