package com.franco.opsengine.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.franco.opsengine");

    @Test
    void domainDoesNotDependOnFrameworks() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "io.swagger..")
                .because("el dominio tiene que poder probarse sin infraestructura");

        rule.check(CLASSES);
    }

    @Test
    void domainDoesNotDependOnApplicationOrAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
                .because("las dependencias apuntan hacia adentro: adapter -> application -> domain");

        rule.check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("los casos de uso dependen de puertos, no de implementaciones");

        rule.check(CLASSES);
    }

    @Test
    void ticketModuleDoesNotDependOnEmployeeModule() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ticket..")
                .should().dependOnClassesThat().resideInAPackage("..employee..")
                .because("los módulos se comunican por su API pública, no por sus internos");

        rule.check(CLASSES);
    }

    @Test
    void employeeModuleDoesNotDependOnTicketModule() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..employee..")
                .should().dependOnClassesThat().resideInAPackage("..ticket..")
                .because("los módulos se comunican por su API pública, no por sus internos");

        rule.check(CLASSES);
    }

    @Test
    void sharedKernelDoesNotDependOnAnyModule() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..shared..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..ticket..", "..employee..", "..team..", "..security..", "..api..")
                .because("shared es el núcleo común: los módulos dependen de él, no al revés");

        rule.check(CLASSES);
    }

    @Test
    void jpaEntitiesLiveOnlyInPersistenceAdapters() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("..adapter.persistence..", "..security..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .because("el mapeo a la base de datos es un detalle del adapter");

        rule.check(CLASSES);
    }
}
