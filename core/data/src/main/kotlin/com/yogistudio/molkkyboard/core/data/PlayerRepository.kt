package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.database.dao.PlayerDao
import com.yogistudio.molkkyboard.core.database.entity.PlayerProfileEntity
import com.yogistudio.molkkyboard.core.database.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profil de joueur local, réutilisable d'une partie à l'autre : un pseudo suffit. L'avatar est un emoji ;
 * [photoUri], facultatif, désigne une photo copiée dans le stockage privé de l'app (jamais envoyée ailleurs).
 */
data class PlayerProfile(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val avatar: String?,
    val photoUri: String? = null,
)

/** Équipe déjà jouée, reconnue par ses joueurs ([memberKey]) : son nom et son palmarès se reprennent. */
data class KnownTeam(
    val memberKey: String,
    val name: String,
    val colorIndex: Int,
    val emblem: String?,
)

interface PlayerRepository {
    /** Profils actifs, par ordre alphabétique. */
    val profiles: Flow<List<PlayerProfile>>

    suspend fun saveProfile(profile: PlayerProfile)

    /** Le profil disparaît des listes ; les parties passées gardent son nom. */
    suspend fun archiveProfile(id: String)

    suspend fun team(memberKey: String): KnownTeam?

    suspend fun saveTeam(team: KnownTeam)
}

@Singleton
class RoomPlayerRepository
    @Inject
    constructor(
        private val dao: PlayerDao,
        private val time: TimeSource,
    ) : PlayerRepository {
        override val profiles: Flow<List<PlayerProfile>> =
            dao.observeProfiles().map { list ->
                list.map { PlayerProfile(it.id, it.name, it.colorIndex, it.avatar, it.photoUri) }
            }

        override suspend fun saveProfile(profile: PlayerProfile) =
            dao.upsertProfile(
                PlayerProfileEntity(
                    id = profile.id,
                    name = profile.name,
                    colorIndex = profile.colorIndex,
                    avatar = profile.avatar,
                    photoUri = profile.photoUri,
                    createdAt = time.now(),
                ),
            )

        override suspend fun archiveProfile(id: String) = dao.archiveProfile(id)

        override suspend fun team(memberKey: String): KnownTeam? =
            dao.team(memberKey)?.let { KnownTeam(it.memberKey, it.name, it.colorIndex, it.emblem) }

        override suspend fun saveTeam(team: KnownTeam) =
            dao.upsertTeam(TeamEntity(team.memberKey, team.name, team.colorIndex, team.emblem, time.now()))
    }
