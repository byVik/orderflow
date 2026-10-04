package dev.viktorstrohush.orderflow.inventory;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** La regla de dependencias de la arquitectura hexagonal, comprobada en cada build. */
@AnalyzeClasses(packages = "dev.viktorstrohush.orderflow.inventory", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule elDominioNoDependeDeFrameworksNiDeOtrasCapas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.apache.kafka..", "com.fasterxml..",
                    "..application..", "..infrastructure..", "dev.viktorstrohush.orderflow.events..");

    @ArchTest
    static final ArchRule laAplicacionNoConoceLaInfraestructura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "jakarta.persistence..", "org.apache.kafka..",
                    "org.springframework.kafka..", "org.springframework.web..", "org.springframework.http..",
                    "dev.viktorstrohush.orderflow.events..");

    @ArchTest
    static final ArchRule losAdaptadoresDeEntradaNoUsanLosDeSalida = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");
}
