import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Shared configuration for Composdown library modules that use Compose Multiplatform.
 *
 * Targets are the same as [KmpLibraryConventionPlugin] except `iosX64`, which Compose Multiplatform
 * no longer publishes.
 */
class ComposeLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            configureKmpLibrary(includeIosX64 = false)
            pluginManager.apply(libs.pluginId("compose-multiplatform"))
            pluginManager.apply(libs.pluginId("kotlin-compose"))
        }
}
