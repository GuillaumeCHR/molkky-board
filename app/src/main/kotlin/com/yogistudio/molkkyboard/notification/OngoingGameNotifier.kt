package com.yogistudio.molkkyboard.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yogistudio.molkkyboard.MainActivity
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification persistante de la partie en cours : numéro du tour, à qui c'est le tour, scores, et l'action
 * « Saisir le lancer » qui rouvre l'écran de jeu. Mise à jour à chaque lancer, retirée à la fin de la partie.
 * Tout est local : aucune notification « push », aucun serveur.
 */
@Singleton
class OngoingGameNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val games: GameRepository,
        private val settings: SettingsRepository,
    ) {
        private val manager = NotificationManagerCompat.from(context)

        @OptIn(ExperimentalCoroutinesApi::class)
        fun start(scope: CoroutineScope) {
            createChannel()
            val ongoing =
                games.observeOngoingGameId().flatMapLatest { id -> if (id == null) flowOf(null) else games.observe(id) }
            scope.launch {
                combine(
                    settings.settings.map { it.ongoingNotification }.distinctUntilChanged(),
                    ongoing,
                ) { enabled, game ->
                    game?.takeIf { enabled }
                }.collect { game -> if (game == null) manager.cancel(NOTIFICATION_ID) else show(game) }
            }
        }

        private fun createChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    setShowBadge(false)
                }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun show(game: GameSnapshot) {
            if (!canNotify()) return
            val state = game.currentSet.replay.state
            val current = state.currentTeam
            val title =
                if (state.status == SetStatus.IN_PROGRESS && current != null) {
                    val who =
                        if (current.players.size >
                            1
                        ) {
                            game.playerName(current.nextPlayer)
                        } else {
                            game.team(current.id)?.name
                        }
                    context.getString(R.string.notification_title, state.round, who ?: current.id.value)
                } else {
                    context.getString(R.string.notification_title_finished)
                }
            val scores =
                state.teams
                    .sortedByDescending { it.score }
                    .joinToString(" · ") { "${game.team(it.id)?.name ?: it.id.value} ${it.score}" }
            val open = openGameIntent(game.id)
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(scores)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(scores))
                    .setContentIntent(open)
                    .addAction(0, context.getString(R.string.notification_action_throw), open)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setShowWhen(false)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
            // Autorisation vérifiée par canNotify() juste au-dessus.
            @Suppress("MissingPermission")
            manager.notify(NOTIFICATION_ID, notification)
        }

        private fun canNotify(): Boolean =
            manager.areNotificationsEnabled() &&
                (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                )

        /** Intention explicite vers l'activité de l'app (aucun lien profond, rien d'exporté en plus). */
        private fun openGameIntent(gameId: String): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_GAME_ID, gameId)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private companion object {
            const val CHANNEL_ID = "ongoing_game"
            const val NOTIFICATION_ID = 1
        }
    }
