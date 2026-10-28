import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.molkkyboard.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.scoring)
    testImplementation(libs.kotlinx.coroutines.test)
}

kover {
    reports {
        filters {
            excludes {
                // Classes sérialisables : leur code est généré par kotlinx-serialization.
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
        verify {
            rule("Validation entièrement couverte") {
                minBound(100, CoverageUnit.LINE)
            }
        }
    }
}
