import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

/**
 * Publishing of a library module to Maven Central as `$COMPOSDOWN_GROUP:<module name>:VERSION_NAME`,
 * with sources, Dokka documentation as the javadoc
 * artifact, and a POM built from `gradle.properties` (shared values at the root, `POM_NAME` and
 * `POM_DESCRIPTION` per module).
 *
 * Publications are signed when a signing key is provided (`signingInMemoryKey`), as on CI for
 * releases; local builds such as `publishToMavenLocal` need no key.
 */
internal fun Project.configurePublishing() {
    pluginManager.apply(libs.pluginId("dokka"))
    pluginManager.apply(libs.pluginId("maven-publish"))

    extensions.configure<MavenPublishBaseExtension> {
        configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = true))
        publishToMavenCentral(automaticRelease = false)
        if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
        pom {
            name.set(property("POM_NAME").toString())
            description.set(property("POM_DESCRIPTION").toString())
            inceptionYear.set("2026")
            url.set(REPOSITORY_URL)
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("springmusk026")
                    name.set("springmusk026")
                    url.set("https://github.com/springmusk026")
                }
            }
            scm {
                url.set(REPOSITORY_URL)
                connection.set("scm:git:git://github.com/springmusk026/Composdown.git")
                developerConnection.set("scm:git:ssh://git@github.com/springmusk026/Composdown.git")
            }
            issueManagement {
                system.set("GitHub")
                url.set("$REPOSITORY_URL/issues")
            }
        }
    }

    extensions.configure<DokkaExtension> {
        moduleName.set(name)
        dokkaSourceSets.configureEach {
            includes.from(layout.projectDirectory.file("Module.md"))
            sourceLink {
                localDirectory.set(layout.projectDirectory.dir("src"))
                remoteUrl("$REPOSITORY_URL/tree/main/$name/src")
                remoteLineSuffix.set("#L")
            }
        }
    }
}

private const val REPOSITORY_URL = "https://github.com/springmusk026/Composdown"
