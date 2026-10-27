package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.replay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Fichier de sauvegarde (et d'export RGPD) : toutes les données locales, en JSON lisible. Les journaux sont
 * repris tels quels (l'état se recalcule) ; les photos de joueurs restent sur le téléphone et ne sont pas incluses.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val app: String,
    val players: List<BackupPlayer> = emptyList(),
    val teams: List<BackupTeam> = emptyList(),
    val games: List<BackupGame> = emptyList(),
) {
    companion object {
        const val FORMAT = "molkky-board-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupPlayer(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val avatar: String? = null,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Serializable
data class BackupTeam(
    val memberKey: String,
    val name: String,
    val colorIndex: Int,
    val emblem: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupGame(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val place: String? = null,
    val config: GameConfig,
    val match: MatchConfig,
    val teams: List<BackupGameTeam>,
    val players: List<BackupGamePlayer> = emptyList(),
    val sets: List<BackupSet>,
    val molkkout: List<MolkkoutThrow> = emptyList(),
)

@Serializable
data class BackupGameTeam(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val emblem: String? = null,
    val memberKey: String,
)

@Serializable
data class BackupGamePlayer(
    val id: String,
    val name: String,
    val profileId: String? = null,
)

@Serializable
data class BackupSet(
    val index: Int,
    val startedAt: Long,
    val lineup: List<TeamLineup>,
    val events: List<BackupEvent> = emptyList(),
)

@Serializable
data class BackupEvent(
    val createdAt: Long,
    val event: SetEvent,
)

/** Écriture lisible et complète (valeurs par défaut comprises : un changement de défaut ne modifie pas les parties). */
internal val BackupJson: Json =
    Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

/**
 * Contrôles d'un fichier venu de l'extérieur, donc non fiable : identifiants et textes bornés, sans caractères de
 * contrôle, manches contiguës, journaux rejouables et cohérents avec les équipes et joueurs de la partie.
 * Une règle par champ ou par bloc du fichier.
 */
@Suppress("TooManyFunctions")
internal object BackupRules {
    const val MAX_BYTES = 20L * 1024 * 1024
    const val MAX_NAME = 40
    const val MAX_PLACE = 100
    const val MAX_SYMBOL = 16
    const val MAX_COLOR = 64
    private val ID = Regex("[A-Za-z0-9_-]{1,64}")

    fun id(value: String?): Boolean = value != null && ID.matches(value)

    fun text(value: String, max: Int = MAX_NAME): Boolean =
        value.isNotBlank() && value.length <= max && value.none { it.isISOControl() }

    fun optional(value: String?, max: Int): Boolean =
        value == null || (value.length <= max && value.none { it.isISOControl() })

    fun color(value: Int): Boolean = value in 0 until MAX_COLOR

    fun player(p: BackupPlayer): Boolean =
        id(p.id) && text(p.name) && optional(p.avatar, MAX_SYMBOL) && color(p.colorIndex)

    fun team(t: BackupTeam): Boolean =
        id(t.memberKey) && text(t.name) && optional(t.emblem, MAX_SYMBOL) && color(t.colorIndex)

    fun game(g: BackupGame): Boolean {
        val teamIds = g.teams.map { it.id }.toSet()
        val playerIds = g.players.map { it.id }.toSet()
        return id(g.id) &&
            optional(g.place, MAX_PLACE) &&
            teams(g.teams, teamIds) &&
            players(g.players, playerIds) &&
            sets(g, teamIds, playerIds) &&
            g.molkkout.all { it.team.value in teamIds && (playerIds.isEmpty() || it.player.value in playerIds) }
    }

    private fun teams(teams: List<BackupGameTeam>, ids: Set<String>): Boolean =
        teams.isNotEmpty() &&
            ids.size == teams.size &&
            teams.all {
                id(it.id) && id(it.memberKey) && text(it.name) && optional(it.emblem, MAX_SYMBOL) &&
                    color(it.colorIndex)
            }

    private fun players(players: List<BackupGamePlayer>, ids: Set<String>): Boolean =
        ids.size == players.size &&
            players.all { id(it.id) && text(it.name) && (it.profileId == null || id(it.profileId)) }

    /** Manches numérotées 0, 1, 2…, événements uniques, compositions cohérentes et journaux rejouables. */
    private fun sets(g: BackupGame, teamIds: Set<String>, playerIds: Set<String>): Boolean {
        val eventIds = g.sets.flatMap { set -> set.events.map { it.event.id.value } }
        return g.sets.isNotEmpty() &&
            g.sets.map { it.index } == g.sets.indices.toList() &&
            eventIds.all(::id) &&
            eventIds.size == eventIds.toSet().size &&
            g.sets.all { set -> lineup(set.lineup, teamIds, playerIds) && replays(g.config, set) }
    }

    /** Les équipes de la manche existent dans la partie ; les joueurs aussi, si la partie les nomme. */
    private fun lineup(lineup: List<TeamLineup>, teams: Set<String>, players: Set<String>): Boolean =
        lineup.isNotEmpty() &&
            lineup.all { team ->
                team.id.value in teams && team.players.isNotEmpty() &&
                    (players.isEmpty() || team.players.all { it.value in players })
            }

    private fun replays(config: GameConfig, set: BackupSet): Boolean =
        runCatching { replay(config, set.lineup, set.events.map { it.event }) is Outcome.Ok }.getOrDefault(false)
}
