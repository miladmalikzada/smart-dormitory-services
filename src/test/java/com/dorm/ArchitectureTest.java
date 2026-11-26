package com.dorm;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Dependencies point inward: ui and web -> service -> domain; infra -> service/domain. */
class ArchitectureTest {
    private static JavaClasses classes;

    @BeforeAll static void load() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.dorm");
    }

    @Test void domainDependsOnNothingInside() {
        noClasses().that().resideInAPackage("com.dorm.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.service..", "com.dorm.infra..", "com.dorm.ui..", "com.dorm.web..", "com.dorm")
                .check(classes);
    }

    @Test void serviceDoesNotKnowInfraOrInterfaces() {
        noClasses().that().resideInAPackage("com.dorm.service..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.infra..", "com.dorm.ui..", "com.dorm.web..", "com.dorm")
                .check(classes);
    }

    @Test void infraDoesNotKnowInterfaces() {
        noClasses().that().resideInAPackage("com.dorm.infra..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.ui..", "com.dorm.web..", "com.dorm")
                .check(classes);
    }

    @Test void uiDoesNotKnowInfraOrWeb() {
        noClasses().that().resideInAPackage("com.dorm.ui..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.infra..", "com.dorm.web..", "com.dorm")
                .check(classes);
    }

    @Test void webDoesNotKnowInfraOrConsoleUi() {
        noClasses().that().resideInAPackage("com.dorm.web..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dorm.infra..", "com.dorm.ui..", "com.dorm")
                .check(classes);
    }
}
