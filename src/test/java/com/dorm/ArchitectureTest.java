package com.dorm;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Dependencies point inward: ui -> service -> domain; infra -> service/domain. */
class ArchitectureTest {
    private static JavaClasses classes;

    @BeforeAll static void load() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.dorm");
    }

    @Test void domainDependsOnNothingInside() {
        noClasses().that().resideInAPackage("com.dorm.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.service..", "com.dorm.infra..", "com.dorm.ui..", "com.dorm")
                .check(classes);
    }

    @Test void serviceDoesNotKnowInfraOrUi() {
        noClasses().that().resideInAPackage("com.dorm.service..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.infra..", "com.dorm.ui..", "com.dorm")
                .check(classes);
    }

    @Test void infraDoesNotKnowUi() {
        noClasses().that().resideInAPackage("com.dorm.infra..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.ui..", "com.dorm")
                .check(classes);
    }

    @Test void uiDoesNotKnowInfra() {
        noClasses().that().resideInAPackage("com.dorm.ui..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.infra..", "com.dorm")
                .check(classes);
    }
}
