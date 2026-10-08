package fr.cours.musical.alarm.clock.api;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "fr.cours.musical.alarm.clock.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_knows_no_framework_nor_other_layer =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..", "..infra..", "org.springframework..",
                            "com.fasterxml..", "tools.jackson..", "java.net.http..")
                    .because("business code must not know any provider, channel or framework detail");

    @ArchTest
    static final ArchRule application_depends_only_on_domain_ports =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infra..", "org.springframework.web..", "org.springframework.http..",
                            "org.springframework.boot..", "org.springframework.beans..",
                            "org.springframework.context..", "com.fasterxml..", "tools.jackson..")
                    .because("services depend on ports, never on adapters, transport or Spring wiring "
                            + "(only the org.springframework.stereotype annotations are tolerated)");

    @ArchTest
    static final ArchRule adapters_do_not_depend_on_each_other =
            slices().matching("..infra.out.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule music_adapters_are_isolated_from_each_other =
            slices().matching("..infra.out.music.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule notification_adapters_are_isolated_from_each_other =
            slices().matching("..infra.out.notification.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule spring_beans_are_never_instantiated_with_new =
            noClasses().should().callConstructorWhere(targetOwner(metaAnnotatedWith(Component.class)))
                    .because("implementations are injected by the container (IoC / DI)");
}
