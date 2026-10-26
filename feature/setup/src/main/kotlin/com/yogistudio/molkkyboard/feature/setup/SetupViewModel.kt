package com.yogistudio.molkkyboard.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupPlayer(
    val id: PlayerId,
    val name: String,
    val colorIndex: Int,
)

data class SetupUiState(
    val players: List<SetupPlayer> = emptyList(),
    val draftName: String = "",
    /** Mode choisi pour cette partie ; Officiel par défaut (`RULES.md` § 8.1). */
    val mode: RulesMode = RulesMode.OFFICIAL,
    /** Mode Officiel : règlement de tournoi IMO. */
    val imo: Boolean = false,
    /** Mode Ludique : exceptions de cette partie (au départ, celles des Réglages). */
    val playful: GameConfig = GameConfig.Playful,
    /** Les exceptions ont été modifiées pour cette partie : les Réglages ne les écrasent plus. */
    val playfulEdited: Boolean = false,
    val creating: Boolean = false,
) {
    val config: GameConfig
        get() =
            when {
                mode == RulesMode.PLAYFUL -> playful
                imo -> GameConfig.ImoTournament
                else -> GameConfig.Classic
            }

    val canStart: Boolean get() = players.size >= MIN_PLAYERS && !creating
    val canAdd: Boolean get() = draftName.isNotBlank()

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_NAME = 20
        const val PALETTE_SIZE = 8
    }
}

@HiltViewModel
class SetupViewModel
    @Inject
    constructor(
        private val games: GameRepository,
        settings: SettingsRepository,
        private val ids: IdGenerator,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                val defaults = settings.settings.first().playfulRules
                mutableState.update { if (it.playfulEdited) it else it.copy(playful = defaults) }
            }
        }

        fun onNameChange(name: String) = mutableState.update { it.copy(draftName = name.take(SetupUiState.MAX_NAME)) }

        /** Ajoute le joueur saisi, avec la première couleur libre de la palette. */
        fun addPlayer() =
            mutableState.update { state ->
                val name = state.draftName.trim()
                if (name.isEmpty()) return@update state
                val used = state.players.map { it.colorIndex }.toSet()
                val color =
                    (0 until SetupUiState.PALETTE_SIZE).firstOrNull { it !in used }
                        ?: (state.players.size % SetupUiState.PALETTE_SIZE)
                state.copy(players = state.players + SetupPlayer(PlayerId(ids.newId()), name, color), draftName = "")
            }

        fun removePlayer(id: PlayerId) =
            mutableState.update { state ->
                state.copy(
                    players =
                        state.players.filterNot {
                            it.id ==
                                id
                        },
                )
            }

        fun shuffle() = mutableState.update { it.copy(players = it.players.shuffled()) }

        fun setMode(mode: RulesMode) = mutableState.update { it.copy(mode = mode) }

        fun setImo(enabled: Boolean) = mutableState.update { it.copy(imo = enabled) }

        /** Exceptions modifiées pour cette partie seulement (les Réglages ne changent pas). */
        fun setPlayful(config: GameConfig) = mutableState.update { it.copy(playful = config, playfulEdited = true) }

        /** Crée la partie (une manche) et renvoie son identifiant à [onCreated]. */
        fun start(onCreated: (String) -> Unit) {
            val state = mutableState.value
            if (!state.canStart) return
            mutableState.update { it.copy(creating = true) }
            viewModelScope.launch {
                val id = games.create(state.toNewGame())
                onCreated(id)
            }
        }

        private fun SetupUiState.toNewGame() =
            NewGame(
                config = config,
                matchConfig = MatchConfig(setsToWin = 1),
                lineup = players.map { TeamLineup(TeamId(it.id.value), listOf(it.id)) },
                teams =
                    players.map {
                        GameTeam(
                            TeamId(it.id.value),
                            it.name,
                            it.colorIndex,
                            memberKey = memberKey(listOf(it.id)),
                        )
                    },
                players = players.map { GamePlayer(it.id, it.name) },
            )
    }
