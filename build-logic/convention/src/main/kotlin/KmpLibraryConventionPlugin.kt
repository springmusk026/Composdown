import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Shared configuration for every pure-Kotlin Composdown multiplatform library module (no Compose).
 *
 * Targets: Android, Desktop (JVM), iosArm64, iosSimulatorArm64 and iosX64.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureKmpLibrary(includeIosX64 = true)
    }
}

/**
 * Applies and configures Kotlin Multiplatform for a Composdown library module.
 *
 * Targets are declared only here, so adding a platform (for example wasmJs) is a change to this one
 * function. Source sets follow Kotlin's default hierarchy template. Every library module uses
 * explicit API mode and treats compiler warnings as errors.
 *
 * @param includeIosX64 whether to build for Intel-Mac iOS simulators. Compose Multiplatform stopped
 *   publishing `iosX64` artifacts after 1.11.0-alpha01, so modules that depend on Compose can't target it.
 */
internal fun Project.configureKmpLibrary(includeIosX64: Boolean) {
    val libs = libs
    pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
    pluginManager.apply(libs.pluginId("android-kmp-library"))
    pluginManager.apply(libs.pluginId("composdown-quality"))

    group = COMPOSDOWN_GROUP
    configurePublishing()

    extensions.getByType<KotlinMultiplatformExtension>().apply {
        explicitApi()

        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
            namespace = androidNamespace()
            compileSdk = libs.version("android-compileSdk").toInt()
            minSdk = libs.version("android-minSdk").toInt()
            withHostTest {}
            compilerOptions.jvmTarget.set(JVM_TARGET)
        }

        jvm("desktop") {
            compilerOptions.jvmTarget.set(JVM_TARGET)
        }

        iosArm64()
        iosSimulatorArm64()
        if (includeIosX64) iosX64()

        compilerOptions {
            allWarningsAsErrors.set(true)
        }

        sourceSets.getByName("commonTest").dependencies {
            implementation(kotlin("test"))
        }
    }
}

/** `composdown-core` becomes `io.github.springmusk026.composdown.core`. */
private fun Project.androidNamespace(): String =
    COMPOSDOWN_PACKAGE + "." + name.removePrefix("composdown-").replace('-', '.')

private val JVM_TARGET = JvmTarget.JVM_17
