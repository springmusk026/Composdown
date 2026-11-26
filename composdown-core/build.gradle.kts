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

val generateHtmlEntities by tasks.registering(GenerateHtmlEntitiesTask::class) {
    entitiesJson.set(layout.projectDirectory.file("data/html-entities.json"))
    packageName.set("io.github.springmusk026.composdown.core.internal.text")
    outputDirectory.set(layout.buildDirectory.dir("generated/htmlEntities/commonMain/kotlin"))
}

val generateCommonMarkSpecTests by tasks.registering(GenerateSpecTestsTask::class) {
    specJson.set(layout.projectDirectory.file("spec/commonmark-0.31.2.json"))
    className.set("CommonMarkSpecTest")
    packageName.set("io.github.springmusk026.composdown.core.spec")
    outputDirectory.set(layout.buildDirectory.dir("generated/specTests/commonTest/kotlin"))
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
        }
    }
}
