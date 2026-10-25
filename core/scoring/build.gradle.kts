import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.molkky.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.model)
    // Couverture de core:model mesurée ici : ses types sont exercés par les tests du moteur.
    kover(projects.core.model)
}

kover {
    reports {
        filters {
            excludes {
                // Classes de données sérialisables : sans logique, leur code est généré par kotlinx-serialization.
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
        verify {
            rule("Moteur de score entièrement couvert") {
                minBound(100, CoverageUnit.LINE)
                // Les seules branches non couvertes sont les tests de nullité générés par Kotlin quand on
                // compare une équipe au `turn` nullable : inatteignables, car `turn` est non nul en cours de manche.
                minBound(98, CoverageUnit.BRANCH)
            }
        }
    }
}
