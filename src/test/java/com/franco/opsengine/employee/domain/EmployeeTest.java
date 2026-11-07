package com.franco.opsengine.employee.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTest {

    private static final Skill ACCESS = new Skill("ACCESS_IDENTITY");
    private static final Skill BILLING = new Skill("BILLING");
    private static final TeamId TEAM = TeamId.newId();

    private static Employee employee(int maxOpenTickets) {
        return Employee.create(EmployeeId.newId(), "Ana Pérez", TEAM, Set.of(ACCESS), maxOpenTickets);
    }

    @Test
    void canTakeATicketWhenAvailableSkilledAndWithinCapacity() {
        assertThat(employee(3).canTake(ACCESS)).isTrue();
    }

    @Test
    void cannotTakeATicketWhenTheSkillIsMissing() {
        assertThat(employee(3).canTake(BILLING)).isFalse();
    }

    @Test
    void cannotTakeATicketWhenUnavailable() {
        Employee employee = employee(3);

        employee.changeAvailability(Availability.UNAVAILABLE);

        assertThat(employee.canTake(ACCESS)).isFalse();
    }

    @ParameterizedTest(name = "with {0} open tickets out of 2, canTake is {1}")
    @CsvSource({"0,true", "1,true", "2,false"})
    void respectsTheCapacityLimit(int openTickets, boolean expected) {
        Employee employee = employee(2);
        for (int i = 0; i < openTickets; i++) {
            employee.takeTicket(ACCESS);
        }

        assertThat(employee.canTake(ACCESS)).isEqualTo(expected);
    }

    @Test
    void takingATicketIncreasesTheLoad() {
        Employee employee = employee(3);

        employee.takeTicket(ACCESS);

        assertThat(employee.openTickets()).isEqualTo(1);
    }

    @Test
    void releasingATicketDecreasesTheLoad() {
        Employee employee = employee(3);
        employee.takeTicket(ACCESS);

        employee.releaseTicket();

        assertThat(employee.openTickets()).isZero();
    }

    @Test
    void takingATicketWhenIneligibleThrowsAndDoesNotChangeTheLoad() {
        Employee employee = employee(3);

        assertThatThrownBy(() -> employee.takeTicket(BILLING))
                .isInstanceOf(IllegalStateException.class);
        assertThat(employee.openTickets()).isZero();
    }

    @Test
    void releasingWithoutOpenTicketsThrows() {
        assertThatThrownBy(() -> employee(3).releaseTicket())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void needsAtLeastOneSkill() {
        assertThatThrownBy(() -> Employee.create(EmployeeId.newId(), "Ana", TEAM, Set.of(), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxOpenTicketsMustBePositive() {
        assertThatThrownBy(() -> Employee.create(EmployeeId.newId(), "Ana", TEAM, Set.of(ACCESS), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void needsATeam() {
        assertThatThrownBy(() -> Employee.create(EmployeeId.newId(), "Ana", null, Set.of(ACCESS), 3))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rebuildRestoresAvailabilityAndLoad() {
        Employee employee = Employee.rebuild(
                EmployeeId.newId(), "Ana", TEAM, Set.of(ACCESS), 5, Availability.UNAVAILABLE, 3);

        assertThat(employee.availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(employee.openTickets()).isEqualTo(3);
        assertThat(employee.canTake(ACCESS)).isFalse();
    }
}
