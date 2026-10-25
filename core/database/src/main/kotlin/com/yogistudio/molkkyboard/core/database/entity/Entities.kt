package com.yogistudio.molkkyboard.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Profil de joueur local, réutilisable d'une partie à l'autre. Un pseudo suffit (minimisation). */
@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int,
    /** Emoji ou identifiant de mascotte ; la photo éventuelle reste sur l'appareil. */
    val avatar: String?,
    val photoUri: String?,
    val createdAt: Long,
    val archived: Boolean = false,
)

/**
 * Identité d'équipe, reconnue par l'ensemble de ses joueurs ([memberKey] = empreinte des identifiants triés) :
 * si les mêmes joueurs se regroupent, l'équipe et son palmarès sont repris.
 */
@Entity(tableName = "team")
data class TeamEntity(
    @PrimaryKey val memberKey: String,
    val name: String,
    val colorIndex: Int,
    val emblem: String?,
    val createdAt: Long,
)

/**
 * Une partie (un match d'une ou plusieurs manches). Aucun score n'est stocké : tout est recalculé à partir du
 * journal. [closedAt] est renseigné quand la partie est terminée ou abandonnée (pour lister la partie en cours).
 */
@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val configJson: String,
    val matchConfigJson: String,
    val place: String?,
    val closedAt: Long?,
)

/** Une manche : sa composition de départ (équipes et ordre de passage), en JSON. */
@Entity(
    tableName = "game_set",
    primaryKeys = ["gameId", "setIndex"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GameSetEntity(
    val gameId: String,
    val setIndex: Int,
    val lineupJson: String,
    val startedAt: Long,
)

/** Apparence d'une équipe dans une partie (nom, couleur, emblème). */
@Entity(
    tableName = "game_team",
    primaryKeys = ["gameId", "teamId"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GameTeamEntity(
    val gameId: String,
    val teamId: String,
    val name: String,
    val colorIndex: Int,
    val emblem: String?,
    val memberKey: String,
)

/**
 * Événement du journal d'une manche, en ajout seul. [localSeq] donne l'ordre sur l'appareil ; [serverSeq] et
 * [syncState] serviront à la synchronisation (Phase 4).
 */
@Entity(
    tableName = "game_event",
    indices = [Index(value = ["gameId", "setIndex", "localSeq"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GameEventEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val setIndex: Int,
    val localSeq: Long,
    val serverSeq: Long? = null,
    val syncState: String = SYNC_LOCAL,
    val createdAt: Long,
    val payloadJson: String,
) {
    companion object {
        /** Partie locale, jamais synchronisée. */
        const val SYNC_LOCAL = "LOCAL"
    }
}

/** Joueur d'une partie : son nom affiché, et le profil local dont il vient (absent pour un invité). */
@Entity(
    tableName = "game_player",
    primaryKeys = ["gameId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GamePlayerEntity(
    val gameId: String,
    val playerId: String,
    val name: String,
    val profileId: String?,
)
