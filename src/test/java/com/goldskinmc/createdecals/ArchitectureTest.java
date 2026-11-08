package com.goldskinmc.createdecals;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
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

    /**
     * Copycats+ is optional and All Rights Reserved: only compat/copycats may touch it (that package loads only when it is
     * installed), and only its foundation package, the part its wiki calls public and stable.
     */
    @Test
    void copycatsPlusOnlyThroughItsFoundationFromCompat() {
        noClasses().that().resideOutsideOfPackage("..createdecals.compat.copycats..")
                .should().dependOnClassesThat().resideInAnyPackage("com.copycatsplus..")
                .because("other packages reach Copycats+ through compat/CopycatsPlus, which checks it is installed")
                .check(mod);
        noClasses().should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage("com.copycatsplus..")
                        .and(JavaClass.Predicates.resideOutsideOfPackage("com.copycatsplus.copycats.foundation..")))
                .because("only Copycats+'s foundation package is public and stable")
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
