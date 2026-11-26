import codegen.GenerateHtmlEntitiesTask

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

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateHtmlEntities)
            dependencies {
                api(libs.kotlinx.collections.immutable)
            }
        }
    }
}
