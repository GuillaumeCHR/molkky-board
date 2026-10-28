package com.yogistudio.molkkyboard.feature.setup

import com.yogistudio.molkkyboard.core.data.KnownTeam
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.designsystem.icon.PLAYER_AVATARS
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.model.nameKey

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

/**
 * Nom à clarifier avant d'ajouter un joueur : deux joueurs ne portent jamais le même nom dans une partie, et un nom
 * déjà connu peut désigner un joueur habituel (ses statistiques continuent alors de s'accumuler).
 */
sealed interface NameQuestion {
    /** Équipe où ajouter le joueur (`null` : l'ajout ordinaire) et équipes proposées en mode équipes. */
    val team: TeamId?
    val teamNames: List<String>

    /** « C'est la même Léa ? » : un joueur habituel porte déjà ce nom. */
    data class SameProfile(
        val profile: PlayerProfile,
        override val team: TeamId?,
        override val teamNames: List<String>,
    ) : NameQuestion

    /** Un autre nom est nécessaire : [taken] est le nom en conflit, [suggestion] le nom proposé (« Léa 2 »). */
    data class Rename(
        val taken: String,
        val suggestion: String,
        /** Le conflit vient de la partie (sinon, d'un joueur habituel qu'on a dit différent). */
        val inGame: Boolean,
        override val team: TeamId?,
        override val teamNames: List<String>,
        /** Le nom proposé est lui aussi déjà pris. */
        val stillTaken: Boolean = false,
    ) : NameQuestion
}

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
    /** Première partie guidée en cours (la bulle « ajoutez les joueurs » s'affiche tant qu'il en manque). */
    val guide: Boolean = false,
    val creating: Boolean = false,
    /** Nom à clarifier avant d'ajouter un joueur (même nom dans la partie, ou joueur habituel homonyme). */
    val nameQuestion: NameQuestion? = null,
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

    /** Deux équipes portent le même nom (majuscules et accents ignorés) : on ne lance pas, pour éviter la confusion. */
    val duplicateTeamName: String?
        get() =
            if (playMode != PlayMode.TEAMS) {
                null
            } else {
                units
                    .groupBy { it.name.nameKey() }
                    .values
                    .firstOrNull { it.size > 1 }
                    ?.first()
                    ?.name
            }

    val canStart: Boolean
        get() =
            units.size >= MIN_PLAYERS && !creating && units.all { it.name.isNotBlank() } &&
                duplicateTeamName == null
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
