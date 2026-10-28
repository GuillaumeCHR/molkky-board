package com.yogistudio.molkkyboard.feature.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.CommandGateway
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.nameKey
import com.yogistudio.molkkyboard.core.model.nextFreeName
import com.yogistudio.molkkyboard.core.nearby.NearbyManager
import com.yogistudio.molkkyboard.core.nearby.NearbyState
import com.yogistudio.molkkyboard.core.nearby.NetworkMode
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.sync.CommandOutcome
import com.yogistudio.molkkyboard.core.sync.ConnectionId
import com.yogistudio.molkkyboard.core.sync.DeviceId
import com.yogistudio.molkkyboard.core.sync.GameCommand
import com.yogistudio.molkkyboard.core.sync.JoinCandidate
import com.yogistudio.molkkyboard.core.sync.SharedPlayer
import com.yogistudio.molkkyboard.core.sync.SharedTeam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Un joueur de la partie, à associer au téléphone qui demande à rejoindre. */
data class PartyPlayer(
    val id: PlayerId,
    val name: String,
)

/** Écrans « Inviter » (hôte) et « Rejoindre » (invité) : l'état de la partie à côté et les actions possibles. */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModel
    @Inject
    constructor(
        private val nearby: NearbyManager,
        private val games: GameRepository,
        private val gateway: CommandGateway,
        private val players: PlayerRepository,
        private val ids: IdGenerator,
    ) : ViewModel() {
        val state: StateFlow<NearbyState> = nearby.state

        /** Joueurs de la partie partagée, sauf ceux déjà associés à un téléphone. */
        val freePlayers: StateFlow<List<PartyPlayer>> =
            nearby.state
                .flatMapLatest { s -> s.gameId?.let { id -> games.observe(id).map { it to s } } ?: flowOf(null) }
                .map { pair ->
                    val (snapshot, s) = pair ?: return@map emptyList()
                    val taken = s.guests.mapNotNull { it.player }.toSet()
                    snapshot
                        ?.players
                        .orEmpty()
                        .filter { it.id !in taken }
                        .map { PartyPlayer(it.id, it.name) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

        /** Pseudo proposé à l'invité : le premier joueur habituel, sinon rien. */
        suspend fun suggestedNickname(): String =
            players.profiles
                .first()
                .firstOrNull()
                ?.name
                .orEmpty()

        fun share(gameId: String, mode: NetworkMode) = nearby.startHosting(gameId, mode)

        fun renewInvite() = nearby.renewInvite()

        /** « C'est Léa » : le téléphone saisira les lancers de ce joueur. */
        fun accept(candidate: JoinCandidate, player: PlayerId) = nearby.accept(candidate.connection, player)

        /** « Nouveau joueur » : il rejoint la partie (comme un retardataire), puis son téléphone est accepté. */
        fun acceptAsNewPlayer(candidate: JoinCandidate) {
            val gameId = state.value.gameId ?: return
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                val player = PlayerId(ids.newId())
                val team = TeamId(player.value)
                val name =
                    newcomerName(snapshot.players.map { it.name } + snapshot.teams.map { it.name }, candidate.nickname)
                val command =
                    GameCommand.AddParticipant(
                        AddPlayer(EventId(ids.newId()), player, team),
                        SharedPlayer(player, name, candidate.avatar),
                        SharedTeam(
                            team,
                            name,
                            freeColor(snapshot.teams.map { it.colorIndex }),
                            memberKey = memberKey(listOf(player)),
                        ),
                        baseSeq = snapshot.currentSet.log.size,
                    )
                if (gateway.submit(gameId, command) ==
                    CommandOutcome.Applied
                ) {
                    nearby.accept(candidate.connection, player)
                }
            }
        }

        fun refuse(candidate: JoinCandidate) = nearby.refuse(candidate.connection)

        fun exclude(device: DeviceId) = nearby.exclude(device)

        fun join(link: String, nickname: String) = nearby.join(link, nickname.trim().ifEmpty { "?" }, null)

        fun stop() = nearby.stop()

        private companion object {
            const val STOP_MS = 5_000L
        }
    }

/** Nom du nouveau venu : son pseudo, sauf s'il est déjà pris dans la partie (« Léa 2 » alors, règle des homonymes). */
internal fun newcomerName(taken: List<String>, wanted: String): String =
    if (taken.none { it.nameKey() == wanted.nameKey() }) wanted else wanted.nextFreeName(taken, MAX_NAME)

/** Première couleur de la palette encore libre ; si les 8 sont prises, on recommence au début. */
internal fun freeColor(used: List<Int>): Int = (0 until PALETTE).firstOrNull { it !in used } ?: (used.size % PALETTE)

private const val MAX_NAME = 20
private const val PALETTE = 8
