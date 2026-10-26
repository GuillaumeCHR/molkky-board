package com.yogistudio.molkkyboard.feature.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.KnownTeam
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Photos de joueurs : interface pour que les tests se passent de fichiers. */
interface PhotoStore {
    /** Fichier où l'appareil photo enregistre la prise de vue. */
    fun captureUri(): Uri

    suspend fun import(source: Uri, playerId: String): String?

    suspend fun delete(playerId: String)
}

// Une fonction par action de l'écran de création : les regrouper rendrait l'interface moins lisible.
@Suppress("TooManyFunctions")
@HiltViewModel
class SetupViewModel
    @Inject
    constructor(
        private val games: GameRepository,
        settings: SettingsRepository,
        private val profiles: PlayerRepository,
        private val photos: PhotoStore,
        private val ids: IdGenerator,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                val defaults = settings.settings.first().playfulRules
                mutableState.update { if (it.playfulEdited) it else it.copy(playful = defaults) }
            }
            viewModelScope.launch {
                profiles.profiles.collect { list ->
                    mutableState.update { it.copy(knownProfiles = list) }
                }
            }
        }

        fun onNameChange(name: String) = mutableState.update { it.copy(draftName = name.take(SetupUiState.MAX_NAME)) }

        /** Ajoute le joueur saisi et crée son profil local (avatar et couleur libres), pour les prochaines parties. */
        fun addPlayer(teamNames: List<String> = emptyList()) {
            val name = mutableState.value.draftName.trim()
            if (name.isEmpty()) return
            val state = mutableState.value
            val profile =
                PlayerProfile(
                    id = ids.newId(),
                    name = name,
                    colorIndex =
                        firstFree(
                            (0 until SetupUiState.PALETTE_SIZE).toList(),
                            state.players.map {
                                it.colorIndex
                            },
                            state.players.size,
                        ),
                    avatar = firstFree(PLAYER_AVATARS, state.players.map { it.avatar }, state.players.size),
                )
            mutableState.update { it.copy(draftName = "") }
            addProfile(profile, teamNames)
            viewModelScope.launch { profiles.saveProfile(profile) }
        }

        /** Ajoute directement un nouveau joueur à une équipe (et crée son profil). */
        fun addPlayerToTeam(team: TeamId, name: String) {
            val trimmed = name.trim().take(SetupUiState.MAX_NAME)
            if (trimmed.isEmpty()) return
            val state = mutableState.value
            val profile =
                PlayerProfile(
                    id = ids.newId(),
                    name = trimmed,
                    colorIndex =
                        firstFree(
                            (0 until SetupUiState.PALETTE_SIZE).toList(),
                            state.players.map {
                                it.colorIndex
                            },
                            state.players.size,
                        ),
                    avatar = firstFree(PLAYER_AVATARS, state.players.map { it.avatar }, state.players.size),
                )
            addProfileToTeam(team, profile)
            viewModelScope.launch { profiles.saveProfile(profile) }
        }

        /** Ajoute un joueur habituel à une équipe précise. */
        fun addProfileToTeam(team: TeamId, profile: PlayerProfile) {
            mutableState.update { state ->
                if (state.player(PlayerId(profile.id)) != null) return@update state
                val player =
                    SetupPlayer(
                        PlayerId(profile.id),
                        profile.name,
                        profile.colorIndex,
                        profile.avatar ?: PLAYER_AVATARS.first(),
                        profile.photoUri,
                    )
                state.copy(
                    players = state.players + player,
                    teams = state.teams.map { if (it.id == team) it.copy(members = it.members + player.id) else it },
                )
            }
            refreshKnownTeams()
        }

        /** Ajoute un joueur habituel ; en équipes, il rejoint l'équipe la moins nombreuse. */
        fun addProfile(profile: PlayerProfile, teamNames: List<String> = emptyList()) {
            mutableState.update { state ->
                if (state.player(PlayerId(profile.id)) != null) return@update state
                val player =
                    SetupPlayer(
                        PlayerId(profile.id),
                        profile.name,
                        profile.colorIndex,
                        profile.avatar ?: PLAYER_AVATARS.first(),
                        profile.photoUri,
                    )
                val teams =
                    if (state.playMode ==
                        PlayMode.TEAMS
                    ) {
                        state.teams.withPlayer(player.id, teamNames)
                    } else {
                        state.teams
                    }
                state.copy(players = state.players + player, teams = teams)
            }
            refreshKnownTeams()
        }

        fun removePlayer(id: PlayerId) {
            mutableState.update { state ->
                state
                    .copy(
                        players = state.players.filterNot { it.id == id },
                        teams = state.teams.map { team -> team.copy(members = team.members - id) },
                        family = state.family - TeamId(id.value),
                    )
            }
            refreshKnownTeams()
        }

        /** Glisser-déposer de l'ordre de passage en solo. */
        fun movePlayer(from: Int, to: Int) = mutableState.update { it.copy(players = it.players.moved(from, to)) }

        fun setAvatar(id: PlayerId, avatar: String) {
            updatePlayer(id) { it.copy(avatar = avatar) }
        }

        /** Photo prise ou choisie : copiée, réduite et débarrassée de ses métadonnées. */
        fun setPhoto(id: PlayerId, source: Uri) {
            viewModelScope.launch {
                val uri = photos.import(source, id.value) ?: return@launch
                updatePlayer(id) { it.copy(photoUri = uri) }
            }
        }

        /** Fichier à transmettre à l'appareil photo. */
        fun captureUri(): Uri = photos.captureUri()

        fun removePhoto(id: PlayerId) {
            viewModelScope.launch { photos.delete(id.value) }
            updatePlayer(id) { it.copy(photoUri = null) }
        }

        private fun updatePlayer(id: PlayerId, change: (SetupPlayer) -> SetupPlayer) {
            mutableState.update { state ->
                state.copy(
                    players =
                        state.players.map {
                            if (it.id ==
                                id
                            ) {
                                change(it)
                            } else {
                                it
                            }
                        },
                )
            }
            val player = mutableState.value.player(id) ?: return
            viewModelScope.launch {
                profiles.saveProfile(
                    PlayerProfile(player.id.value, player.name, player.colorIndex, player.avatar, player.photoUri),
                )
            }
        }

        /** Solo ou équipes ; en passant en équipes, les joueurs sont répartis en alternance dans deux équipes. */
        fun setPlayMode(mode: PlayMode, teamNames: List<String> = emptyList()) {
            mutableState.update { state ->
                if (mode == state.playMode) return@update state
                val teams =
                    if (mode == PlayMode.TEAMS && state.teams.isEmpty()) {
                        val two = buildList { repeat(2) { add(newTeam(size, teamNames, this)) } }
                        state.players.foldIndexed(two) { index, acc, p ->
                            acc.mapIndexed { i, team ->
                                if (i ==
                                    index % acc.size
                                ) {
                                    team.copy(members = team.members + p.id)
                                } else {
                                    team
                                }
                            }
                        }
                    } else {
                        state.teams
                    }
                state.copy(playMode = mode, teams = teams, family = emptyMap())
            }
            refreshKnownTeams()
        }

        fun addTeam(teamNames: List<String> = emptyList()) =
            mutableState.update { state ->
                state.copy(
                    teams =
                        state.teams + newTeam(state.teams.size, teamNames, state.teams),
                )
            }

        /** Supprime une équipe ; ses joueurs rejoignent l'équipe la moins nombreuse. */
        fun removeTeam(id: TeamId) {
            mutableState.update { state ->
                val removed = state.teams.firstOrNull { it.id == id } ?: return@update state
                val rest = state.teams.filterNot { it.id == id }
                val teams = removed.members.fold(rest) { acc, player -> acc.withPlayer(player, emptyList()) }
                state.copy(teams = teams, family = state.family - id)
            }
            refreshKnownTeams()
        }

        fun renameTeam(id: TeamId, name: String) =
            updateTeam(
                id,
            ) { it.copy(name = name.take(MAX_TEAM_NAME), known = null) }

        /** 🎲 : un nom amusant au hasard, différent de ceux déjà pris. */
        fun randomTeamName(id: TeamId, teamNames: List<String>) {
            val taken =
                mutableState.value.teams
                    .map { it.name }
                    .toSet()
            val name = teamNames.filterNot { it in taken }.randomOrNull() ?: return
            updateTeam(id) { it.copy(name = name) }
        }

        fun setTeamColor(id: TeamId, color: Int) = updateTeam(id) { it.copy(colorIndex = color) }

        fun setTeamEmblem(id: TeamId, emblem: String) = updateTeam(id) { it.copy(emblem = emblem) }

        /** Glisser-déposer de l'ordre de passage des équipes. */
        fun moveTeam(from: Int, to: Int) = mutableState.update { it.copy(teams = it.teams.moved(from, to)) }

        /** Glisser-déposer de la rotation des lanceurs dans une équipe. */
        fun moveMember(
            team: TeamId,
            from: Int,
            to: Int,
        ) = updateTeam(team) { it.copy(members = it.members.moved(from, to)) }

        /** Fait passer un joueur dans l'équipe suivante. */
        fun switchTeam(player: PlayerId) {
            mutableState.update { state ->
                val index = state.teams.indexOfFirst { player in it.members }
                if (index < 0 || state.teams.size < 2) return@update state
                val target = (index + 1) % state.teams.size
                state.copy(
                    teams =
                        state.teams.mapIndexed { i, team ->
                            when (i) {
                                index -> team.copy(members = team.members - player)
                                target -> team.copy(members = team.members + player)
                                else -> team
                            }
                        },
                )
            }
            refreshKnownTeams()
        }

        /** Reprend le nom, la couleur et l'emblème de l'équipe déjà jouée par ces joueurs (son palmarès s'accumule). */
        fun acceptKnownTeam(id: TeamId) =
            updateTeam(id) { team ->
                val known = team.known ?: return@updateTeam team
                team.copy(
                    name = known.name,
                    colorIndex = known.colorIndex,
                    emblem = known.emblem ?: team.emblem,
                    known = null,
                )
            }

        fun dismissKnownTeam(id: TeamId) = updateTeam(id) { it.copy(known = null) }

        fun shuffle() =
            mutableState.update { state ->
                when (state.playMode) {
                    PlayMode.SOLO -> {
                        state.copy(players = state.players.shuffled())
                    }

                    PlayMode.TEAMS -> {
                        state.copy(
                            teams = state.teams.shuffled().map { it.copy(members = it.members.shuffled()) },
                        )
                    }
                }
            }

        fun setMode(mode: RulesMode) = mutableState.update { it.copy(mode = mode) }

        fun setImo(enabled: Boolean) = mutableState.update { it.copy(imo = enabled) }

        /** Exceptions modifiées pour cette partie seulement (les Réglages ne changent pas). */
        fun setPlayful(config: GameConfig) = mutableState.update { it.copy(playful = config, playfulEdited = true) }

        /** Mode famille : surcharge d'un joueur (solo) ou d'une équipe. */
        fun setFamily(id: TeamId, override: TeamOverride) =
            mutableState.update {
                it.copy(
                    family =
                        if (override ==
                            TeamOverride()
                        ) {
                            it.family - id
                        } else {
                            it.family + (id to override)
                        },
                )
            }

        /** Crée la partie (une manche) et renvoie son identifiant à [onCreated]. */
        fun start(onCreated: (String) -> Unit) {
            val state = mutableState.value
            if (!state.canStart) return
            mutableState.update { it.copy(creating = true) }
            viewModelScope.launch {
                if (state.playMode == PlayMode.TEAMS) {
                    state.teams.filter { it.members.isNotEmpty() }.forEach {
                        profiles.saveTeam(KnownTeam(memberKey(it.members), it.name.trim(), it.colorIndex, it.emblem))
                    }
                }
                val id = games.create(state.toNewGame())
                onCreated(id)
            }
        }

        private fun SetupUiState.toNewGame() =
            NewGame(
                config = config,
                matchConfig = MatchConfig(setsToWin = 1),
                lineup = lineup,
                teams =
                    units.map { unit ->
                        val emblem = teams.firstOrNull { it.id == unit.id }?.emblem
                        GameTeam(unit.id, unit.name.trim(), unit.colorIndex, emblem, memberKey(unit.players))
                    },
                players = players.map { GamePlayer(it.id, it.name, profileId = it.id.value) },
            )

        private fun updateTeam(id: TeamId, change: (SetupTeam) -> SetupTeam) =
            mutableState.update { state -> state.copy(teams = state.teams.map { if (it.id == id) change(it) else it }) }

        private fun newTeam(index: Int, teamNames: List<String>, existing: List<SetupTeam>) =
            SetupTeam(
                id = TeamId(ids.newId()),
                name = freeName(teamNames, existing, index),
                colorIndex =
                    firstFree(
                        (0 until SetupUiState.PALETTE_SIZE).toList(),
                        existing.map { it.colorIndex },
                        index,
                    ),
                emblem = firstFree(TEAM_EMBLEMS.keys.toList(), existing.map { it.emblem }, index),
            )

        private fun freeName(teamNames: List<String>, existing: List<SetupTeam>, index: Int): String =
            teamNames.filterNot { name -> existing.any { it.name == name } }.randomOrNull() ?: "${index + 1}"

        /** Le joueur rejoint l'équipe la moins nombreuse (la première, à égalité). */
        private fun List<SetupTeam>.withPlayer(player: PlayerId, teamNames: List<String>): List<SetupTeam> {
            val teams = ifEmpty { listOf(newTeam(0, teamNames, emptyList())) }
            val target = teams.minBy { it.members.size }.id
            return teams.map { if (it.id == target) it.copy(members = it.members + player) else it }
        }

        /** Cherche, pour chaque équipe, si ces mêmes joueurs ont déjà joué ensemble sous un autre nom. */
        private fun refreshKnownTeams() {
            val teams = mutableState.value.teams
            viewModelScope.launch {
                teams.filter { it.members.isNotEmpty() }.forEach { team ->
                    val known = profiles.team(memberKey(team.members))?.takeIf { it.name != team.name }
                    mutableState.update { state ->
                        state.copy(
                            teams =
                                state.teams.map {
                                    if (it.id == team.id && it.members == team.members) it.copy(known = known) else it
                                },
                        )
                    }
                }
            }
        }

        private companion object {
            const val MAX_TEAM_NAME = 30
        }
    }
