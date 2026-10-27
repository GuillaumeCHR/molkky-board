package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.MatchStatus
import com.yogistudio.molkkyboard.core.scoring.Molkkout
import com.yogistudio.molkkyboard.core.scoring.MolkkoutState
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.SetResult
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.getOrNull
import com.yogistudio.molkkyboard.core.scoring.matchStatus
import com.yogistudio.molkkyboard.core.scoring.toResult
import java.security.MessageDigest

/** Apparence d'une équipe dans une partie. */
data class GameTeam(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val emblem: String? = null,
    val memberKey: String,
)

/** Joueur d'une partie : son nom affiché et, s'il vient d'un profil local, l'identifiant de ce profil. */
data class GamePlayer(
    val id: PlayerId,
    val name: String,
    val profileId: String? = null,
)

/** Ce qu'il faut pour créer une partie : règles, composition de la 1re manche et apparence des équipes. */
data class NewGame(
    val config: GameConfig,
    val matchConfig: MatchConfig,
    val lineup: List<TeamLineup>,
    val teams: List<GameTeam>,
    val players: List<GamePlayer> = emptyList(),
    val place: String? = null,
)

/** Une manche : composition de départ, journal brut et état obtenu par rejeu. */
data class GameSetSnapshot(
    val index: Int,
    val lineup: List<TeamLineup>,
    val log: List<SetEvent>,
    val replay: ReplayResult,
)

/** Une partie telle que l'interface l'affiche ; rien n'y est stocké en dur, tout vient du journal. */
data class GameSnapshot(
    val id: String,
    val createdAt: Long,
    val closedAt: Long?,
    val config: GameConfig,
    val matchConfig: MatchConfig,
    val teams: List<GameTeam>,
    val sets: List<GameSetSnapshot>,
    val place: String?,
    val players: List<GamePlayer> = emptyList(),
    /** Lancers du Mölkkout éventuel (départage avant la belle). */
    val molkkout: List<MolkkoutThrow> = emptyList(),
) {
    val currentSet: GameSetSnapshot get() = sets.last()

    val abandoned: Boolean get() = sets.any { it.replay.state.status == SetStatus.ABANDONED }

    /** Manches terminées, dans l'ordre : base du match. */
    private val results: List<SetResult> get() = sets.mapNotNull { it.replay.state.toResult() }

    /** Ordre de passage de la 1re manche : référence du match. */
    private val firstOrder: List<TeamId> get() = sets.first().lineup.map { it.id }

    /** État du Mölkkout (seulement si un match l'exige). */
    val molkkoutState: MolkkoutState?
        get() = molkkoutTeams()?.let { Molkkout.replay(it, molkkout).getOrNull() }

    /**
     * Où en est le match, une fois la manche en cours terminée : `null` tant qu'elle se joue (ou si elle est
     * abandonnée). Une partie d'une seule manche est un match en 1 manche gagnante.
     */
    val match: MatchStatus?
        get() {
            if (currentSet.replay.state.status != SetStatus.FINISHED) return null
            return matchStatus(matchConfig, firstOrder, results, molkkoutState?.winner)
        }

    /** Équipes du Mölkkout, dans l'ordre de la 1re manche, quand le match en demande un. */
    fun molkkoutTeams(): List<TeamLineup>? {
        if (currentSet.replay.state.status != SetStatus.FINISHED) return null
        val status = matchStatus(matchConfig, firstOrder, results)
        if (status !is MatchStatus.MolkkoutRequired) return null
        val last = currentSet.replay.state
        return firstOrder.map { id -> TeamLineup(id, last.team(id)?.players.orEmpty()) }
    }

    /** Victoires de manche de chaque équipe dans le match. */
    val setWins: Map<TeamId, Int> get() = results.groupingBy { it.winner }.eachCount()

    private val playerNames by lazy { players.associate { it.id to it.name } }
    private val teamsById by lazy { teams.associateBy { it.id } }

    fun playerName(id: PlayerId): String = playerNames[id] ?: id.value

    fun team(id: TeamId): GameTeam? = teamsById[id]
}

/**
 * Identité d'une équipe : empreinte SHA-256 de ses identifiants de joueurs triés. Les mêmes joueurs regroupés
 * retrouvent la même équipe (nom, couleur, palmarès).
 */
fun memberKey(players: Collection<PlayerId>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    players.map { it.value }.sorted().forEach {
        digest.update(it.toByteArray(Charsets.UTF_8))
        digest.update(0)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
