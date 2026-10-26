package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.ThrowResult

/** Une équipe (ou un joueur seul) telle que l'écran l'affiche. */
data class TeamUi(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val score: Int,
    val misses: Int,
    val active: Boolean,
    val out: Boolean,
    /** Hors jeu par élimination (sinon, l'équipe est partie). */
    val eliminated: Boolean = false,
    /** Points qu'il reste à marquer pour atteindre la cible. */
    val remaining: Int = 0,
    /** Joueurs de l'équipe (vide ou un seul nom pour un joueur seul). */
    val members: List<String> = emptyList(),
    /** Identifiants des joueurs, dans le même ordre que [members]. */
    val memberIds: List<PlayerId> = emptyList(),
)

/** Nature d'une ligne du déroulé. */
enum class LogKind { THROW, FAULT, WRONG_TEAM, JOINED, LEFT, REORDER, ABANDON }

/**
 * Une ligne du déroulé : un événement du journal effectif, attribué à son équipe et à son joueur (attribution
 * par position), avec le score avant et après. Seuls les lancers se modifient ; lancers et fautes se suppriment.
 */
data class LogItemUi(
    val id: EventId,
    val round: Int,
    val kind: LogKind,
    val teamName: String? = null,
    val colorIndex: Int = 0,
    val playerName: String? = null,
    val result: ThrowResult? = null,
    val points: Int = 0,
    val scoreBefore: Int? = null,
    val scoreAfter: Int? = null,
    val faultKind: FaultKind? = null,
) {
    val editable: Boolean get() = kind == LogKind.THROW
    val deletable: Boolean get() = kind == LogKind.THROW || kind == LogKind.FAULT
}

/** Les lignes du déroulé d'un même tour. */
data class LogRound(
    val round: Int,
    val items: List<LogItemUi>,
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
    /** Tour en cours, dérivé du journal (`SCORING_SPEC.md` § 16). */
    val round: Int = 1,
    /** Le prochain raté de l'équipe au tour serait sanctionné (élimination ou retour en arrière). */
    val missWarning: Boolean = false,
    /** Personne n'a encore lancé dans le tour : l'ordre de passage peut changer. */
    val betweenRounds: Boolean = true,
    val inputMode: InputMode = InputMode.PINS,
    val quickMode: QuickPadMode = QuickPadMode.SinglePin,
    val selection: Selection = Selection.None,
    val preview: Preview = Preview(),
    val canUndo: Boolean = false,
    val finished: Boolean = false,
    val abandoned: Boolean = false,
    val winnerName: String? = null,
    val ranking: List<TeamUi> = emptyList(),
    /** Déroulé chronologique, groupé par tour. */
    val log: List<LogRound> = emptyList(),
) {
    /** Le déroulé se modifie tant que la partie n'est pas abandonnée (même terminée : la victoire peut changer). */
    val canEditLog: Boolean get() = !abandoned

    val canValidate: Boolean get() = selection != Selection.None && !finished && !abandoned

    /** Tableau des scores : équipes en jeu par score décroissant (à égalité, ordre de passage), puis hors jeu. */
    val scoreboard: List<TeamUi> get() =
        teams.filterNot { it.out }.sortedByDescending { it.score } +
            teams.filter { it.out }
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
        val team: TeamUi,
        val missLimit: Int,
    ) : GameEffect

    /** Une annulation ou une correction a défait une élimination : la séquence se rejoue à l'envers. */
    data class EliminationCancelled(
        val team: TeamUi,
        val missLimit: Int,
    ) : GameEffect

    /** Lancer de 12 points : la quille 12 seule, ou les 12 quilles d'un coup ([strike]). */
    data class Twelve(
        val strike: Boolean,
    ) : GameEffect

    /** Correction ou suppression enregistrée : tout a été recalculé. */
    data object Edited : GameEffect

    /** Un retardataire a rejoint la partie (il démarre à 0, en fin d'ordre). */
    data class PlayerJoined(
        val name: String,
    ) : GameEffect

    /** Un joueur a quitté la partie. */
    data class PlayerLeft(
        val name: String,
    ) : GameEffect

    /** Nouvel ordre de passage enregistré. */
    data object OrderChanged : GameEffect

    /** Correction ou suppression refusée par le moteur (journal inchangé). */
    data class EditRefused(
        val error: ScoringError,
    ) : GameEffect

    data class Won(
        val name: String,
    ) : GameEffect
}
