package com.goldskinmc.createdecals;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Enforces the prototype's dependency rules, so breaking one fails the build. */
class ArchitectureTest {
    private static JavaClasses mod;

    @BeforeAll
    static void importModClasses() {
        mod = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.goldskinmc.createdecals");
    }

    @Test
    void createIsTheOnlyModDependency() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage("dev.eriksonn..", "dev.simulated_team..", "dev.ryanhcode..",
                        "net.irisshaders..", "net.caffeinemc..")
                .because("decals live in the block model: Sable vehicles, Sodium and Iris must need nothing of their own")
                .check(mod);
    }

    @Test
    void commonCodeStaysOffTheClient() {
        noClasses().that().resideInAnyPackage("..createdecals.decal..", "..createdecals.block..", "..createdecals.command..",
                        "..createdecals.registry..")
                .should().dependOnClassesThat().resideInAnyPackage("net.minecraft.client..", "..createdecals.client..")
                .check(mod);
    }
}
