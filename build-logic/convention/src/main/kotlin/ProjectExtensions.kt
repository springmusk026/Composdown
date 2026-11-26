import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import java.util.Optional

/** The `libs` version catalog shared by the build and the convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).require("version", alias).requiredVersion

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).require("library", alias)

internal fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).require("plugin", alias).get().pluginId

private fun <T : Any> Optional<T>.require(
    kind: String,
    alias: String,
): T = orElseThrow { IllegalStateException("Missing $kind '$alias' in gradle/libs.versions.toml") }

/** Group ID for every published Composdown artifact. */
internal const val COMPOSDOWN_GROUP = "io.github.springmusk026"

/** Root package for all Composdown modules. */
internal const val COMPOSDOWN_PACKAGE = "io.github.springmusk026.composdown"
