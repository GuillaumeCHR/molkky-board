import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.molkkyboard.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.scoring)
}

kover {
    reports {
        verify {
            rule("Validation entièrement couverte") {
                minBound(100, CoverageUnit.LINE)
            }
        }
    }
}
