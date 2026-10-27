package com.yogistudio.molkkyboard.feature.setup

import com.yogistudio.molkkyboard.core.data.KnownTeam
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.TeamOverride

/** Partie en solo (chacun pour soi) ou en équipes nommées. */
enum class PlayMode { SOLO, TEAMS }

/** Un joueur de la partie, issu d'un profil local (créé à la volée si besoin). */
data class SetupPlayer(
    val id: PlayerId,
    val name: String,
    val colorIndex: Int,
    val avatar: String = PLAYER_AVATARS.first(),
    val photoUri: String? = null,
)

/**
 * Une équipe nommée : nom, couleur, emblème et joueurs dans leur ordre de rotation.
 *
 * @property known équipe déjà jouée par exactement ces joueurs, proposée en reprise (nom et palmarès).
 */
data class SetupTeam(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val emblem: String,
    val members: List<PlayerId> = emptyList(),
    val known: KnownTeam? = null,
)

/** Une unité de jeu (joueur seul ou équipe), telle qu'elle lancera : sert à l'ordre et au mode famille. */
data class SetupUnit(
    val id: TeamId,
    val name: String,
    val colorIndex: Int,
    val players: List<PlayerId>,
)

data class SetupUiState(
    val playMode: PlayMode = PlayMode.SOLO,
    /** Joueurs de la partie ; en solo, c'est l'ordre de passage. */
    val players: List<SetupPlayer> = emptyList(),
    /** Équipes, dans l'ordre de passage (mode équipes). */
    val teams: List<SetupTeam> = emptyList(),
    /** Tous les profils locaux enregistrés. */
    val knownProfiles: List<PlayerProfile> = emptyList(),
    val draftName: String = "",
    /** Mode choisi pour cette partie ; Officiel par défaut (`RULES.md` § 8.1). */
    val mode: RulesMode = RulesMode.OFFICIAL,
    /** Mode Officiel : règlement de tournoi IMO. */
    val imo: Boolean = false,
    /** Mode Ludique : exceptions de cette partie (au départ, celles des Réglages). */
    val playful: GameConfig = GameConfig.Playful,
    /** Les exceptions ont été modifiées pour cette partie : les Réglages ne les écrasent plus. */
    val playfulEdited: Boolean = false,
    /** Mode famille (Ludique seulement) : score de départ, cible et exemption par joueur ou par équipe. */
    val family: Map<TeamId, TeamOverride> = emptyMap(),
    /** Format : une manche (1) ou un match en 2 manches gagnantes (belle comprise). */
    val setsToWin: Int = 1,
    val creating: Boolean = false,
) {
    fun player(id: PlayerId): SetupPlayer? = players.firstOrNull { it.id == id }

    /** Profils locaux qui ne sont pas encore dans la partie, proposés en un tap. */
    val profiles: List<PlayerProfile> get() =
        knownProfiles.filter { profile ->
            players.none { it.id.value == profile.id }
        }

    /** Joueurs ou équipes non vides, dans l'ordre où ils lanceront. */
    val units: List<SetupUnit>
        get() =
            when (playMode) {
                PlayMode.SOLO -> {
                    players.map { SetupUnit(TeamId(it.id.value), it.name, it.colorIndex, listOf(it.id)) }
                }

                PlayMode.TEAMS -> {
                    teams.filter { it.members.isNotEmpty() }.map {
                        SetupUnit(
                            it.id,
                            it.name,
                            it.colorIndex,
                            it.members,
                        )
                    }
                }
            }

    val lineup: List<TeamLineup> get() = units.map { TeamLineup(it.id, it.players) }

    /** Règles de la partie ; le mode famille ne s'applique qu'en Ludique, et seulement aux unités présentes. */
    val config: GameConfig
        get() =
            when {
                mode == RulesMode.PLAYFUL -> {
                    val present = units.map { it.id }.toSet()
                    playful.copy(teamOverrides = family.filter { (id, o) -> id in present && o != TeamOverride() })
                }

                imo -> {
                    GameConfig.ImoTournament
                }

                else -> {
                    GameConfig.Classic
                }
            }

    val canStart: Boolean get() = units.size >= MIN_PLAYERS && !creating && units.all { it.name.isNotBlank() }
    val canAdd: Boolean get() = draftName.isNotBlank()

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_NAME = 20
        const val PALETTE_SIZE = 8
    }
}

/** Mode famille : score de départ borné sous la cible, cible bornée comme celle du mode Ludique. */
fun TeamOverride.withStartScore(score: Int, target: Int): TeamOverride =
    copy(startScore = score.coerceIn(0, (targetScore ?: target) - 1).takeIf { it > 0 })

fun TeamOverride.withTarget(target: Int, defaultTarget: Int): TeamOverride {
    val bounded = target.coerceIn(PlayfulRules.MIN_TARGET, PlayfulRules.MAX_TARGET)
    val start = startScore?.coerceAtMost(bounded - 1)
    return copy(targetScore = bounded.takeIf { it != defaultTarget }, startScore = start?.takeIf { it > 0 })
}
