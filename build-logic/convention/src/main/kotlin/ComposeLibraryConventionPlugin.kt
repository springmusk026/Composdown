import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Shared configuration for Composdown library modules that use Compose Multiplatform.
 *
 * Targets are the same as [KmpLibraryConventionPlugin] except `iosX64`, which Compose Multiplatform
 * no longer publishes. On wasmJs, tests are not run: Compose needs a browser there, and rendering is
 * tested on the other platforms.
 */
class ComposeLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            configureKmpLibrary(usesCompose = true)
            pluginManager.apply(libs.pluginId("compose-multiplatform"))
            pluginManager.apply(libs.pluginId("kotlin-compose"))
            // This check is for Compose UI tests that run in a browser; this module's tests do not run
            // on wasmJs (see configureKmpLibrary).
            tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
        }
}
