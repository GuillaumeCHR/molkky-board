package com.yogistudio.molkkyboard.navigation

import androidx.lifecycle.ViewModel
import com.yogistudio.molkkyboard.core.nearby.NearbyManager
import com.yogistudio.molkkyboard.core.nearby.NearbyRole
import com.yogistudio.molkkyboard.core.nearby.NearbyState
import com.yogistudio.molkkyboard.feature.game.ConnectionBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** État de la partie à côté, pour la pastille de connexion de l'écran de jeu (qui ne dépend pas de ce module). */
@HiltViewModel
class SharingViewModel
    @Inject
    constructor(
        nearby: NearbyManager,
    ) : ViewModel() {
        val state: StateFlow<NearbyState> = nearby.state
    }

/** « 3 téléphones » chez l'hôte, « Hors ligne » chez un invité déconnecté ; rien si la partie n'est pas partagée. */
internal fun NearbyState.badgeFor(gameId: String): ConnectionBadge? =
    when {
        this.gameId != gameId -> null
        role == NearbyRole.HOST && link != null -> ConnectionBadge(phones, online = true)
        role == NearbyRole.GUEST -> ConnectionBadge(phones = null, online = connected)
        else -> null
    }
