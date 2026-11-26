import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Formatting (Spotless with ktlint and the Compose rules ruleset) and static analysis (detekt).
 *
 * Style settings come from the root `.editorconfig`, and detekt settings from
 * `config/detekt/detekt.yml`. `check` runs both.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            val libs = libs
            pluginManager.apply(libs.pluginId("spotless"))
            pluginManager.apply(libs.pluginId("detekt"))

            val ktlintVersion = libs.version("ktlint")
            val composeRules = libs.library("ktlint-compose-rules").get().toString()

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    targetExclude("**/build/**")
                    ktlint(ktlintVersion)
                        .setEditorConfigPath(rootProject.file(".editorconfig"))
                        .customRuleSets(listOf(composeRules))
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(ktlintVersion)
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                parallel = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom("src")
            }

            // detekt 1.23.x bundles its own Kotlin compiler and fails at runtime when Gradle aligns it
            // with the build's newer Kotlin version, so its classpath is pinned to the version it was
            // built against. This only affects the analysis tool, not the code under analysis.
            val detektKotlin = libs.version("detekt-kotlin")
            configurations.matching { it.name == "detekt" }.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlin)
                }
            }

            tasks.withType<Detekt>().configureEach {
                jvmTarget = "17"
                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    xml.required.set(false)
                    txt.required.set(false)
                    md.required.set(false)
                }
            }
        }
}
