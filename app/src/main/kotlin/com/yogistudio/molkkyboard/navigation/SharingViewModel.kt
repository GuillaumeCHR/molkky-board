package com.yogistudio.molkkyboard.navigation

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import com.yogistudio.molkkyboard.core.nearby.NearbyManager
import com.yogistudio.molkkyboard.core.nearby.NearbyNotice
import com.yogistudio.molkkyboard.core.nearby.NearbyRole
import com.yogistudio.molkkyboard.core.nearby.NearbyState
import com.yogistudio.molkkyboard.core.nearby.R
import com.yogistudio.molkkyboard.core.sync.GuestStatus
import com.yogistudio.molkkyboard.feature.game.ConnectionBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** État de la partie à côté, pour la pastille de connexion de l'écran de jeu (qui ne dépend pas de ce module). */
@HiltViewModel
class SharingViewModel
    @Inject
    constructor(
        private val nearby: NearbyManager,
    ) : ViewModel() {
        val state: StateFlow<NearbyState> = nearby.state

        /** Départs, exclusions et fin du partage, annoncés sur l'écran de jeu. */
        val notices: SharedFlow<NearbyNotice> = nearby.notices

        /** Invité : quitte la partie partagée ; elle reste consultable sur ce téléphone. */
        fun leave() = nearby.stop()
    }

/** « 3 téléphones » chez l'hôte, « Hors ligne » chez un invité déconnecté ; rien si la partie n'est pas partagée. */
internal fun NearbyState.badgeFor(gameId: String): ConnectionBadge? =
    when {
        this.gameId != gameId -> {
            null
        }

        role == NearbyRole.HOST && link != null -> {
            ConnectionBadge(phones, online = true)
        }

        role == NearbyRole.GUEST && guestStatus == GuestStatus.PLAYING -> {
            ConnectionBadge(
                phones = null,
                online = connected,
                player = player,
            )
        }

        else -> {
            null
        }
    }

/** Texte d'un avertissement de la partie à côté. */
internal fun NearbyNotice.text(resources: Resources): String =
    when (this) {
        is NearbyNotice.GuestLeft -> {
            resources.getString(R.string.notice_guest_left, name)
        }

        is NearbyNotice.GuestOffline -> {
            resources.getString(R.string.notice_guest_offline, name)
        }

        is NearbyNotice.GuestBack -> {
            resources.getString(R.string.notice_guest_back, name)
        }

        NearbyNotice.RemovedByHost -> {
            resources.getString(R.string.notice_removed)
        }

        NearbyNotice.SharingStopped -> {
            resources.getString(R.string.notice_stopped)
        }

        is NearbyNotice.GameOver -> {
            resources.getString(
                if (host) R.string.notice_over_host else R.string.notice_over_guest,
            )
        }
    }
