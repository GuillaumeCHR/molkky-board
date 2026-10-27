package com.yogistudio.molkkyboard.feature.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.StatsRepository
import com.yogistudio.molkkyboard.core.data.toStatsGame
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.stats.Moment
import com.yogistudio.molkkyboard.core.stats.isNewRecord
import com.yogistudio.molkkyboard.core.stats.moments
import com.yogistudio.molkkyboard.core.stats.summarize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Type de moment, pour n'en proposer qu'un de chaque sorte (le strike et le « 12 » seul restent distincts). */
private val Moment.kind: String get() =
    if (this is Moment.Twelve) {
        "twelve-$strike"
    } else {
        this::class
            .simpleName
            .orEmpty()
    }

/** Réglages de la vignette choisis par l'auteur. */
data class ShareChoices(
    val momentIndex: Int = 0,
    val template: ShareTemplate? = null,
    val format: ShareFormat = ShareFormat.STORY,
    /** Équipes dont le nom est masqué (« Joueur 2 ») : vie privée des autres joueurs. */
    val masked: Set<TeamId> = emptySet(),
    val text: String? = null,
)

data class ShareUiState(
    val loading: Boolean = true,
    val snapshot: GameSnapshot? = null,
    val moments: List<Moment> = emptyList(),
    val choices: ShareChoices = ShareChoices(),
    val avatars: Map<String, String> = emptyMap(),
) {
    val moment: Moment? get() = moments.getOrNull(choices.momentIndex)
    val victory: Moment.Victory? get() = moments.filterIsInstance<Moment.Victory>().firstOrNull()

    /** Modèle affiché : celui choisi, sinon celui qui convient au moment. */
    val template: ShareTemplate get() = choices.template ?: moment?.defaultTemplate() ?: ShareTemplate.PODIUM

    /** Équipes qu'on peut masquer (toutes : l'auteur peut se masquer lui-même). */
    val teams: List<Pair<TeamId, String>>
        get() =
            snapshot
                ?.let { s ->
                    s.currentSet.replay.state.teams
                        .map { it.id to (s.team(it.id)?.name ?: it.id.value) }
                }.orEmpty()
}

/**
 * Partage d'une partie : moments détectés depuis le journal (le plus fort en premier), modèle, format, noms
 * masquables. Le partage est toujours proposé, jamais imposé.
 */
@HiltViewModel
class ShareViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        games: GameRepository,
        stats: StatsRepository,
        players: PlayerRepository,
    ) : ViewModel() {
        private val gameId: String = checkNotNull(savedStateHandle[GAME_ID])
        private val choices = MutableStateFlow(ShareChoices())

        val state: StateFlow<ShareUiState> =
            combine(
                games.observe(gameId).filterNotNull(),
                stats.summaries,
                choices,
                players.profiles.map { list -> list.mapNotNull { p -> p.avatar?.let { p.id to it } }.toMap() },
            ) { snapshot, history, chosen, avatars ->
                val game = snapshot.toStatsGame()
                val index = snapshot.currentSet.index
                val added = summarize(game).firstOrNull { it.setIndex == index }
                val previous = history.filterNot { it.gameId == gameId }
                val record = added != null && isNewRecord(previous, added)
                // Un seul moment par type (quatre « 12 » d'une partie parfaite ne font qu'une vignette).
                val distinct = moments(game, index, record).distinctBy { it.kind }
                ShareUiState(false, snapshot, distinct, chosen, avatars)
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShareUiState())

        fun selectMoment(index: Int) = choices.update { it.copy(momentIndex = index, template = null, text = null) }

        fun selectTemplate(template: ShareTemplate) = choices.update { it.copy(template = template) }

        fun selectFormat(format: ShareFormat) = choices.update { it.copy(format = format) }

        fun toggleMasked(team: TeamId) =
            choices.update { it.copy(masked = if (team in it.masked) it.masked - team else it.masked + team) }

        fun editText(text: String) = choices.update { it.copy(text = text.take(MAX_TEXT)) }

        companion object {
            const val GAME_ID = "gameId"
            private const val STOP_TIMEOUT_MS = 5_000L
            private const val MAX_TEXT = 280
        }
    }
