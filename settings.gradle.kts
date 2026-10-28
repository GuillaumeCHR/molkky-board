pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MolkkyBoard"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":core:model")
include(":core:scoring")
include(":core:stats")
include(":core:sync")
include(":core:nearby")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":feature:intro")
include(":feature:setup")
include(":feature:game")
include(":feature:history")
include(":feature:stats")
include(":feature:share")
