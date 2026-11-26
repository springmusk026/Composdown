pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "composdown"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":composdown-core")
include(":composdown-compose")
include(":composdown-coil")
include(":composdown-highlight")
include(":benchmarks")
include(":sample:shared")
include(":sample:desktop")
include(":sample:android")
include(":tools:screenshots")
