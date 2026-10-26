package com.yogistudio.molkkyboard.core.model

// Règles dérivées de la configuration. Elles vivent hors de la classe @Serializable pour que la
// couverture de tests mesure ce code-là et non le code généré par kotlinx-serialization.

/** Une partie avec handicap compte dans les victoires mais pas dans les records « purs ». */
val GameConfig.handicap: Boolean get() = teamOverrides.isNotEmpty()

/** Officiel s'il s'agit exactement des règles officielles, avec ou sans le règlement IMO ; Ludique sinon. */
val GameConfig.mode: RulesMode
    get() =
        if (this == GameConfig.Classic ||
            this == GameConfig.ImoTournament
        ) {
            RulesMode.OFFICIAL
        } else {
            RulesMode.PLAYFUL
        }

/** Règles « pures » (records, victoire la plus rapide) : seulement en mode Officiel (`SCORING_SPEC.md` § 17). */
val GameConfig.isStandard: Boolean get() = mode == RulesMode.OFFICIAL

fun GameConfig.targetFor(team: TeamId): Int = teamOverrides[team]?.targetScore ?: targetScore

fun GameConfig.startScoreFor(team: TeamId): Int = teamOverrides[team]?.startScore ?: 0

fun GameConfig.isExemptFromMissLimit(team: TeamId): Boolean = teamOverrides[team]?.exemptFromMissLimit ?: false
