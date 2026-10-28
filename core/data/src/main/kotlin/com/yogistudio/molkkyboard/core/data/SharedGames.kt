package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.sync.Author
import com.yogistudio.molkkyboard.core.sync.Authority
import com.yogistudio.molkkyboard.core.sync.CommandOutcome
import com.yogistudio.molkkyboard.core.sync.DeviceId
import com.yogistudio.molkkyboard.core.sync.GameCommand
import com.yogistudio.molkkyboard.core.sync.GuestStore
import com.yogistudio.molkkyboard.core.sync.RejectCode
import com.yogistudio.molkkyboard.core.sync.Role
import com.yogistudio.molkkyboard.core.sync.SharedGame
import kotlinx.coroutines.flow.first
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autorité de l'hôte d'une partie à côté : la partie en cours sur ce téléphone, par la même porte que toute saisie
 * ([GameRepository.apply]). Une revanche fait passer la session à la nouvelle partie.
 */
class GameAuthority(
    private val games: GameRepository,
    gameId: String,
    private val avatars: suspend () -> Map<String, String> = { emptyMap() },
) : Authority {
    @Volatile
    var gameId: String = gameId
        private set

    override suspend fun apply(command: GameCommand, author: Author): CommandOutcome =
        games.apply(gameId, command, author).also {
            if (it == CommandOutcome.Applied && command is GameCommand.Rematch) gameId = command.gameId
        }

    override suspend fun snapshot(): SharedGame = checkNotNull(games.share(gameId, avatars())) { "partie introuvable" }
}

/**
 * Ce que fait un invité de ce que l'hôte diffuse : la partie reçue est importée telle quelle, puis chaque commande
 * est rejouée par la même porte (au nom du téléphone auteur ; l'hôte l'a déjà validée).
 */
class GuestGameStore(
    private val games: GameRepository,
) : GuestStore {
    @Volatile
    var gameId: String? = null
        private set

    override suspend fun importGame(game: SharedGame) {
        games.importShared(game)
        gameId = game.id
    }

    override suspend fun applyRemote(command: GameCommand, author: String) {
        val id = gameId ?: return
        val outcome = games.apply(id, command, Author(DeviceId(author), Role.ORGANIZER))
        if (outcome == CommandOutcome.Applied && command is GameCommand.Rematch) gameId = command.gameId
    }
}

/** Routage des commandes d'une partie partagée : fourni par le module de la partie à côté. */
interface NearbyRouter {
    /** Cette partie est partagée en ce moment (hébergée ou rejointe). */
    fun routes(gameId: String): Boolean

    suspend fun submit(gameId: String, command: GameCommand): CommandOutcome
}

/**
 * Unique chemin des saisies de l'app : une partie partagée passe par l'hôte (autorité), une partie locale par le
 * dépôt. Les écrans ne savent pas lequel des deux s'applique.
 */
@Singleton
class CommandGateway
    @Inject
    constructor(
        private val games: GameRepository,
        private val router: Optional<NearbyRouter>,
    ) {
        suspend fun submit(gameId: String, command: GameCommand): CommandOutcome {
            val nearby = router.orElse(null)
            return when {
                nearby != null && nearby.routes(gameId) -> nearby.submit(gameId, command)

                // Partie rejointe sans session (hôte parti, app redémarrée) : lecture seule, elle divergerait sinon.
                games
                    .observe(
                        gameId,
                    ).first()
                    ?.origin == GameOrigin.JOINED -> CommandOutcome.Rejected(RejectCode.FORBIDDEN)

                else -> games.apply(gameId, command)
            }
        }
    }
