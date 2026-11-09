package de.aboutlabs.damagedetector.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.ACCESS_STANDARD_STREAMS;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.Label;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.jev.OpenRouterJev;
import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** DDD-17 / brief.md "Enforcement": the eight architecture rules over the production classes. Every active rule blocks the build. */
class ArchitectureRulesTest {

    private static final String ROOT = "de.aboutlabs.damagedetector";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String DETECTOR_LAUNCHER = ROOT + ".DetectorLauncher";
    private static final String DETECTOR_CONFIGURATION = ROOT + ".DetectorConfiguration";
    private static final String DETECTOR_COMMAND = ROOT + ".cli.DetectorCommand";
    private static final String IMAGE_FOLDER = ROOT + ".inspection.ImageFolder";
    private static final String OUTPUT_FOLDER = ROOT + ".inspection.OutputFolder";
    private static final String RETRYING_JEV = ROOT + ".jev.RetryingJev";
    private static final String PASS_THRESHOLD = ROOT + ".cli.PassThreshold";

    private static final JavaClasses PRODUCTION = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT);

    @Test
    void rule_1_domain_depends_only_on_itself_and_the_jdk_basics() {
        classes().that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java.lang..", "java.util..", "java.time..")
            .check(PRODUCTION);
    }

    @Test
    void rule_2_inspection_and_jev_never_see_labels_or_the_use_cases() {
        noClasses().that().resideInAnyPackage(ROOT + ".inspection..", ROOT + ".jev..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".evaluate..", ROOT + ".batch..", ROOT + ".cli..")
            .orShould().dependOnClassesThat().belongToAnyOf(Label.class)
            .check(PRODUCTION);
    }

    @Test
    void rule_2_image_folder_exposes_only_relative_path_strings_and_bytes() {
        methods().that().areDeclaredIn(ImageFolder.class).and().arePublic()
            .should(returnOnlyPathStringsBytesOrProbes())
            .check(PRODUCTION);
    }

    @Test
    void rule_3_only_the_two_printers_use_the_console() {
        noClasses().that(not(partOf(DETECTOR_LAUNCHER, DETECTOR_COMMAND)))
            .should(ACCESS_STANDARD_STREAMS)
            .check(PRODUCTION);
    }

    @Test
    void rule_4_every_startup_check_is_a_probe() {
        classes().that(named(IMAGE_FOLDER, OUTPUT_FOLDER, RETRYING_JEV, PASS_THRESHOLD))
            .should().implement(Probe.class)
            .check(PRODUCTION);
    }

    @Test
    void rule_5_only_the_configuration_constructs_the_open_router_adapter() {
        noClasses().that(not(partOf(DETECTOR_CONFIGURATION)))
            .should().accessTargetWhere(constructorOf(OpenRouterJev.class))
            .check(PRODUCTION);
    }

    @Test
    void rule_5_retrying_jev_is_the_only_jev() {
        classes().that().implement(Jev.class)
            .should(beNamed(RETRYING_JEV))
            .check(PRODUCTION);
        classes().that(named(RETRYING_JEV))
            .should().implement(Jev.class)
            .check(PRODUCTION);
    }

    @Test
    void rule_6_only_the_output_folder_writes() {
        noClasses().that(not(partOf(OUTPUT_FOLDER)))
            .should().accessTargetWhere(calling(Files.class, "write", "writeString", "copy", "move", "createDirectory",
                "createDirectories", "createFile", "createTempFile", "delete", "deleteIfExists", "newOutputStream",
                "newBufferedWriter", "newByteChannel"))
            .orShould().accessTargetWhere(constructorOf(FileOutputStream.class, FileWriter.class, RandomAccessFile.class))
            .orShould().accessTargetWhere(printWriterOnAFile())
            .orShould().accessTargetWhere(calling(File.class, "delete", "mkdir", "mkdirs", "renameTo", "createNewFile",
                "setWritable"))
            .orShould().accessTargetWhere(jacksonWriteValueToAFile())
            .check(PRODUCTION);
    }

    @Test
    void rule_7_only_the_image_folder_reads() {
        noClasses().that(not(partOf(IMAGE_FOLDER)))
            .should().accessTargetWhere(calling(Files.class, "walk", "list", "find", "lines", "readAllBytes", "readString",
                "readAllLines", "newInputStream", "newBufferedReader"))
            .orShould().accessTargetWhere(constructorOf(FileInputStream.class, FileReader.class))
            .check(PRODUCTION);
    }

    @Test
    void rule_7_only_the_two_folders_inspect_paths() {
        noClasses().that(not(partOf(IMAGE_FOLDER, OUTPUT_FOLDER)))
            .should().accessTargetWhere(calling(Path.class, "toRealPath"))
            .orShould().accessTargetWhere(calling(Files.class, "exists", "notExists", "isDirectory", "isRegularFile",
                "isSameFile"))
            .check(PRODUCTION);
    }

    @Test
    void rule_8_dependencies_follow_the_allowed_edges() {
        allowedEdges()
            .check(PRODUCTION);
    }

    private static LayeredArchitecture allowedEdges() {
        return layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Root").definedBy(ROOT)
            .layer("Cli").definedBy(ROOT + ".cli..")
            .layer("Batch").definedBy(ROOT + ".batch..")
            .layer("Evaluate").definedBy(ROOT + ".evaluate..")
            .layer("Inspection").definedBy(ROOT + ".inspection..")
            .layer("Jev").definedBy(ROOT + ".jev..")
            .layer("Probe").definedBy(ROOT + ".probe..")
            .layer("Domain").definedBy(DOMAIN)
            .whereLayer("Cli").mayOnlyAccessLayers("Batch", "Evaluate", "Inspection", "Probe", "Domain")
            .whereLayer("Batch").mayOnlyAccessLayers("Inspection", "Domain")
            .whereLayer("Evaluate").mayOnlyAccessLayers("Inspection", "Domain")
            .whereLayer("Inspection").mayOnlyAccessLayers("Domain", "Probe")
            .whereLayer("Jev").mayOnlyAccessLayers("Domain", "Probe")
            .whereLayer("Probe").mayNotAccessAnyLayer()
            .whereLayer("Domain").mayNotAccessAnyLayer();
    }

    /** The named top-level classes, including their nested classes and lambdas. */
    private static DescribedPredicate<JavaClass> partOf(String... topLevelNames) {
        Set<String> names = Set.of(topLevelNames);
        return describe("part of " + names, javaClass -> names.contains(javaClass.getName().split("\\$")[0]));
    }

    private static DescribedPredicate<JavaClass> named(String... names) {
        Set<String> exact = Set.of(names);
        return describe("named " + exact, javaClass -> exact.contains(javaClass.getName()));
    }

    private static DescribedPredicate<JavaAccess<?>> calling(Class<?> owner, String... methodNames) {
        Set<String> names = Set.of(methodNames);
        return describe(owner.getSimpleName() + "." + names, access ->
            access.getTargetOwner().isEquivalentTo(owner) && names.contains(access.getName()));
    }

    private static DescribedPredicate<JavaAccess<?>> constructorOf(Class<?>... owners) {
        List<Class<?>> types = List.of(owners);
        return describe("new " + types, access -> access.getName().equals("<init>")
            && types.stream().anyMatch(access.getTargetOwner()::isEquivalentTo));
    }

    private static DescribedPredicate<JavaAccess<?>> printWriterOnAFile() {
        return describe("new PrintWriter(File|String)", access -> constructorOf(PrintWriter.class).test(access)
            && firstParameterIsAnyOf(access, File.class, String.class));
    }

    private static DescribedPredicate<JavaAccess<?>> jacksonWriteValueToAFile() {
        return describe("Jackson writeValue(File|Path|OutputStream)", access -> access.getName().equals("writeValue")
            && isJackson(access.getTargetOwner())
            && firstParameterIsAnyOf(access, File.class, Path.class, OutputStream.class));
    }

    private static boolean isJackson(JavaClass owner) {
        return owner.getPackageName().startsWith("tools.jackson") || owner.getPackageName().startsWith("com.fasterxml.jackson");
    }

    private static boolean firstParameterIsAnyOf(JavaAccess<?> access, Class<?>... types) {
        if (!(access.getTarget() instanceof CodeUnitAccessTarget target)) {
            return false;
        }
        List<JavaClass> parameters = target.getRawParameterTypes();
        return !parameters.isEmpty() && List.of(types).stream().anyMatch(parameters.get(0)::isEquivalentTo);
    }

    private static ArchCondition<JavaClass> beNamed(String name) {
        return new ArchCondition<>("be " + name) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean satisfied = javaClass.getName().equals(name);
                events.add(new SimpleConditionEvent(javaClass, satisfied, javaClass.getName() + " implements Jev"));
            }
        };
    }

    private static ArchCondition<JavaMethod> returnOnlyPathStringsBytesOrProbes() {
        return new ArchCondition<>("return only relative path strings, bytes or a probe result") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean satisfied = isAllowed(method.getReturnType());
                events.add(new SimpleConditionEvent(method, satisfied,
                    method.getFullName() + " returns " + method.getReturnType().getName()));
            }
        };
    }

    private static boolean isAllowed(JavaType type) {
        if (type instanceof JavaParameterizedType parameterized && type.toErasure().isEquivalentTo(List.class)) {
            return parameterized.getActualTypeArguments().stream().allMatch(argument -> argument.toErasure().isEquivalentTo(String.class));
        }
        JavaClass raw = type.toErasure();
        return Set.of(String.class, byte[].class, ProbeResult.class, ImageFolder.class).stream().anyMatch(raw::isEquivalentTo);
    }
}
