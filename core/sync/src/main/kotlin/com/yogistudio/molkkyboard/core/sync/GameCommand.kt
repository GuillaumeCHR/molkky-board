package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tout ce qu'un téléphone peut demander pour une partie : un événement de la manche en cours, ou une action de match.
 * Chaque commande porte de quoi être **idempotente** face aux doublons et aux saisies concurrentes : la manche à
 * ouvrir, le rang du lancer de Mölkkout, l'identifiant de la nouvelle partie. C'est le contenu des propositions du
 * protocole de `core:sync` (phase 3 bis), validé par l'autorité.
 */
@Serializable
sealed interface GameCommand {
    /** Événement de la manche en cours ; [baseSeq] : taille du journal que le téléphone connaissait. */
    @Serializable
    @SerialName("event")
    data class Play(
        val event: SetEvent,
        val baseSeq: Int,
    ) : GameCommand

    /**
     * Un joueur rejoint la partie en cours ([event]), avec son nom et son équipe (nouvelle ou existante) : l'événement
     * seul ne suffit pas aux autres téléphones pour l'afficher.
     */
    @Serializable
    @SerialName("add_participant")
    data class AddParticipant(
        val event: AddPlayer,
        val player: SharedPlayer,
        val team: SharedTeam,
        val baseSeq: Int,
    ) : GameCommand

    /** Ouvrir la manche numéro [setIndex] (0 = la première), dans l'ordre que le match impose. */
    @Serializable
    @SerialName("next_set")
    data class NextSet(
        val setIndex: Int,
    ) : GameCommand

    /** Lancer de Mölkkout de rang [seq] (1 = le premier). */
    @Serializable
    @SerialName("molkkout_throw")
    data class MolkkoutPlay(
        val seq: Int,
        val throwEvent: MolkkoutThrow,
    ) : GameCommand

    /** Annuler le lancer de Mölkkout de rang [seq], qui doit être le dernier. */
    @Serializable
    @SerialName("molkkout_undo")
    data class MolkkoutUndo(
        val seq: Int,
    ) : GameCommand

    /** Revanche : nouvelle partie [gameId] (identifiant choisi par le demandeur), ordre de passage selon [order]. */
    @Serializable
    @SerialName("rematch")
    data class Rematch(
        val gameId: String,
        val order: NextStarter,
    ) : GameCommand
}

/** Résultat d'une commande : appliquée, ou refusée avec un motif et, le cas échéant, l'erreur du moteur. */
sealed interface CommandOutcome {
    data object Applied : CommandOutcome

    data class Rejected(
        val code: RejectCode,
        val error: ScoringError? = null,
    ) : CommandOutcome
}

/**
 * Droits d'un téléphone sur une commande de match. Les événements de manche ([GameCommand.Play]) sont jugés par
 * [validate] ; ouvrir une manche, annuler un lancer de Mölkkout et lancer une revanche reviennent à l'organisateur ;
 * un joueur saisit son propre lancer de Mölkkout.
 */
fun GameCommand.allowedFor(author: Author): Boolean =
    author.role == Role.ORGANIZER ||
        when (this) {
            is GameCommand.Play -> true

            is GameCommand.MolkkoutPlay -> author.player != null && throwEvent.player == author.player

            is GameCommand.NextSet,
            is GameCommand.MolkkoutUndo,
            is GameCommand.Rematch,
            is GameCommand.AddParticipant,
            -> false
        }
