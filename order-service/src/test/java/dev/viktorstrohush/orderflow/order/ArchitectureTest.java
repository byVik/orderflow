package dev.viktorstrohush.orderflow.order;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** The dependency rule of the hexagonal architecture, checked on every build. */
@AnalyzeClasses(packages = "dev.viktorstrohush.orderflow.order", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule theDomainDoesNotDependOnFrameworksOrOtherLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.apache.kafka..", "com.fasterxml..",
                    "..application..", "..infrastructure..", "dev.viktorstrohush.orderflow.events..");

    @ArchTest
    static final ArchRule theApplicationDoesNotKnowTheInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "jakarta.persistence..", "org.apache.kafka..",
                    "org.springframework.kafka..", "org.springframework.web..", "org.springframework.http..",
                    "dev.viktorstrohush.orderflow.events..");

    @ArchTest
    static final ArchRule theInboundAdaptersDoNotUseTheOutboundOnes = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");
}
