import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.molkkyboard.jvm.library)
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
                // Réglages utilisateur : testés avec DataStore (core:datastore), hors du périmètre du moteur.
                classes("com.yogistudio.molkkyboard.core.model.UserSettings", "com.yogistudio.molkkyboard.core.model.InputMode")
            }
        }
        verify {
            rule("Moteur de score entièrement couvert") {
                minBound(100, CoverageUnit.LINE)
                // Les seules branches non couvertes sont inatteignables : tests de nullité générés par Kotlin sur
                // des identifiants (`turn` est non nul en cours de manche) et cas « liste vide » de `minOf`/`maxOf`
                // (une manche en cours a toujours au moins deux équipes en lice).
                minBound(97, CoverageUnit.BRANCH)
            }
        }
    }
}
