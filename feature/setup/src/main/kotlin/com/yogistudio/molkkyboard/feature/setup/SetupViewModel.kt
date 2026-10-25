package com.yogistudio.molkkyboard.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Préréglage de règles proposé à la création (les règles avancées arrivent avec la 2e livraison du J4). */
enum class RulesPreset(
    val config: GameConfig,
) {
    Classic(GameConfig.Classic),
    Tournament(GameConfig.ImoTournament),
}

data class SetupPlayer(
    val id: PlayerId,
    val name: String,
    val colorIndex: Int,
)

data class SetupUiState(
    val players: List<SetupPlayer> = emptyList(),
    val draftName: String = "",
    val preset: RulesPreset = RulesPreset.Classic,
    val creating: Boolean = false,
) {
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
        private val ids: IdGenerator,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = mutableState.asStateFlow()

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

        fun setPreset(preset: RulesPreset) = mutableState.update { it.copy(preset = preset) }

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
                config = preset.config,
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
