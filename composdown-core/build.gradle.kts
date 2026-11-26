import codegen.GenerateHtmlEntitiesTask
import codegen.GenerateSpecTestsTask

plugins {
    alias(libs.plugins.composdown.kmp.library)
    alias(libs.plugins.poko)
}

poko {
    // Use Composdown's own internal annotation so the published library has no runtime dependency on
    // Poko's annotations artifact.
    pokoAnnotation.set("io/github/springmusk026/composdown/core/internal/Poko")
}

val generateHtmlEntities =
    tasks.register<GenerateHtmlEntitiesTask>("generateHtmlEntities") {
        entitiesJson.set(layout.projectDirectory.file("data/html-entities.json"))
        packageName.set("io.github.springmusk026.composdown.core.internal.text")
        outputDirectory.set(layout.buildDirectory.dir("generated/htmlEntities/commonMain/kotlin"))
    }

val generateCommonMarkSpecTests =
    tasks.register<GenerateSpecTestsTask>("generateCommonMarkSpecTests") {
        specJson.set(layout.projectDirectory.file("spec/commonmark-0.31.2.json"))
        className.set("CommonMarkSpecTest")
        packageName.set("io.github.springmusk026.composdown.core.spec")
        assertion.set("assertCommonMarkExample")
        outputDirectory.set(layout.buildDirectory.dir("generated/commonMarkSpecTests/commonTest/kotlin"))
    }

val generateGfmSpecTests =
    tasks.register<GenerateSpecTestsTask>("generateGfmSpecTests") {
        specJson.set(layout.projectDirectory.file("spec/gfm-0.29.0.gfm.14.json"))
        className.set("GfmSpecTest")
        packageName.set("io.github.springmusk026.composdown.core.spec")
        assertion.set("assertGfmExample")
        outputDirectory.set(layout.buildDirectory.dir("generated/gfmSpecTests/commonTest/kotlin"))
    }

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateHtmlEntities)
            dependencies {
                api(libs.kotlinx.collections.immutable)
            }
        }
        commonTest {
            kotlin.srcDir(generateCommonMarkSpecTests)
            kotlin.srcDir(generateGfmSpecTests)
        }
    }
}

// Android Lint reads all Kotlin source directories, including generated ones, but does not depend on
// the tasks that generate them.
tasks.matching { it.name.startsWith("lintAnalyze") || it.name.endsWith("LintModel") }.configureEach {
    dependsOn(generateHtmlEntities, generateCommonMarkSpecTests, generateGfmSpecTests)
}
