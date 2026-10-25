package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.SetEvent
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
) {
    val currentSet: GameSetSnapshot get() = sets.last()

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
