package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.sync.RejectCode

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
    /** Avatar affiché : l'emoji du joueur seul, ou l'emblème de l'équipe ; `null` → initiale. */
    val avatar: String? = null,
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
    /** Scores de chaque joueur ou équipe à la fin du tour (ou au moment présent pour le tour en cours). */
    val standings: List<Pair<String, Int>> = emptyList(),
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
    /** Partie rejointe sur un autre téléphone (partie à côté) : on n'y invite pas d'autres téléphones. */
    val joined: Boolean = false,
    /** Sanction qui attend l'équipe au tour si [missWarning] (élimination ou retour à un score). */
    val missPenalty: MissPenalty? = null,
    /** Personne n'a encore lancé dans le tour : l'ordre de passage peut changer. */
    val betweenRounds: Boolean = true,
    /** Nouvel ordre demandé en plein tour, appliqué au début du tour suivant. */
    val pendingOrder: List<TeamId>? = null,
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
    /** Fin de la manche (vainqueur, statistiques, match, série) ; `null` tant qu'elle se joue. */
    val end: EndUi? = null,
    val voice: VoicePrefs = VoicePrefs(),
    /** Première partie guidée : bulle à montrer, ou `null`. */
    val guide: GuideStep? = null,
    /** Carte « Prêts à jouer ? » à afficher au début de la partie. */
    val readyCard: Boolean = false,
    /** Réglage « Sons » (désactivé par défaut). */
    val sounds: Boolean = false,
) {
    /** Le déroulé se modifie tant que la partie n'est pas abandonnée (même terminée : la victoire peut changer). */
    val canEditLog: Boolean get() = !abandoned

    val canValidate: Boolean get() = selection != Selection.None && !finished && !abandoned

    /** Tableau des scores : équipes en jeu par score décroissant (à égalité, ordre de passage), puis hors jeu. */
    val scoreboard: List<TeamUi> get() =
        teams.filterNot { it.out }.sortedByDescending { it.score } +
            teams.filter { it.out }
}

/** Étapes de la prise en main sur l'écran de jeu (la 1re, « ajouter les joueurs », est à la création). */
enum class GuideStep {
    /** Toucher les quilles tombées (ou les points, en saisie rapide). */
    PINS,

    /** « Valider » enregistre le lancer. */
    VALIDATE,

    /** Le bouton ↶ annule le dernier lancer. */
    UNDO,
    ;

    /** Numéro affiché : l'étape 1 est à la création de partie. */
    val number: Int get() = ordinal + 2

    companion object {
        /** Étapes en tout, création de partie comprise. */
        val COUNT = entries.size + 1
    }
}

/** Réglages des annonces vocales. */
data class VoicePrefs(
    val enabled: Boolean = false,
    val rate: Float = 1f,
    val volume: Float = 1f,
)

/** Effets ponctuels d'une validation (animations, vibrations, snackbar). */
sealed interface GameEffect {
    data class Validated(
        val name: String,
        val points: Int,
        /** Quilles touchées (mode « Quilles ») : elles s'envolent ; vide en saisie rapide ou sur un raté. */
        val pins: Set<Int> = emptySet(),
    ) : GameEffect

    /** Dépassement de la cible par [team] : de [from] points (au-delà de la cible), retour au score [resetTo]. */
    data class Overshoot(
        val resetTo: Int,
        val team: TeamUi? = null,
        val from: Int = resetTo,
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

    /** Nouvel ordre demandé en plein tour : il s'appliquera au début du tour [round]. */
    data class OrderScheduled(
        val round: Int,
    ) : GameEffect

    /** Saisie refusée (moteur, ou hôte d'une partie à côté) : le journal est inchangé. */
    data class EditRefused(
        val error: ScoringError,
        val code: RejectCode = RejectCode.INVALID,
    ) : GameEffect

    data class Won(
        val name: String,
        val colorIndex: Int,
        val target: Int,
        val newRecord: Boolean,
        val perfect: Boolean,
    ) : GameEffect

    /** Une nouvelle partie a été créée (revanche) : l'écran s'y rend. */
    data class OpenGame(
        val gameId: String,
    ) : GameEffect

    /** Une annonce vocale (si elles sont activées). */
    data class Announce(
        val text: Announcement,
    ) : GameEffect
}

/** Annonces vocales, traduites par l'écran (chaînes externalisées). */
sealed interface Announcement {
    data class Round(
        val round: Int,
    ) : Announcement

    /** « Au tour de Léa », avec ce qu'il lui faut quand la victoire est à portée d'un lancer. */
    data class Turn(
        val name: String,
        val needed: Int?,
    ) : Announcement

    data class Reset(
        val score: Int,
    ) : Announcement

    data class Eliminated(
        val name: String,
    ) : Announcement

    data class Victory(
        val name: String,
        val team: Boolean,
    ) : Announcement

    data object Twelve : Announcement
}
