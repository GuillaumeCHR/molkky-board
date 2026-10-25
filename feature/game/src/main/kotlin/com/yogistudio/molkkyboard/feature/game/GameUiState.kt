package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.TeamId

/** Une équipe (ou un joueur seul) telle que l'écran l'affiche. */
data class TeamUi(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val score: Int,
    val misses: Int,
    val active: Boolean,
    val out: Boolean,
)

/** Sélection en cours, jamais enregistrée tant qu'on n'a pas validé. */
sealed interface Selection {
    data object None : Selection

    data object Miss : Selection

    data class Pins(
        val pins: Set<Int>,
    ) : Selection

    data class Quick(
        val value: Int,
    ) : Selection
}

enum class PreviewKind { NONE, MISS, ELIMINATION, POINTS, OVERSHOOT, WIN }

/** Aperçu en direct du lancer sélectionné : « +7 → 38 ». */
data class Preview(
    val kind: PreviewKind = PreviewKind.NONE,
    val points: Int = 0,
    val newScore: Int = 0,
    val misses: Int = 0,
)

data class GameUiState(
    val loading: Boolean = true,
    val teams: List<TeamUi> = emptyList(),
    val currentTeam: TeamUi? = null,
    val currentPlayerName: String? = null,
    val target: Int = 50,
    val missLimit: Int = 3,
    val inputMode: InputMode = InputMode.PINS,
    val quickMode: QuickPadMode = QuickPadMode.SinglePin,
    val selection: Selection = Selection.None,
    val preview: Preview = Preview(),
    val canUndo: Boolean = false,
    val finished: Boolean = false,
    val abandoned: Boolean = false,
    val winnerName: String? = null,
    val ranking: List<TeamUi> = emptyList(),
) {
    val canValidate: Boolean get() = selection != Selection.None && !finished && !abandoned
}

/** Effets ponctuels d'une validation (animations, vibrations, snackbar). */
sealed interface GameEffect {
    data class Validated(
        val name: String,
        val points: Int,
    ) : GameEffect

    /** Dépassement de la cible : retour au score [resetTo]. */
    data class Overshoot(
        val resetTo: Int,
    ) : GameEffect

    data class Eliminated(
        val name: String,
    ) : GameEffect

    data class Won(
        val name: String,
    ) : GameEffect
}
