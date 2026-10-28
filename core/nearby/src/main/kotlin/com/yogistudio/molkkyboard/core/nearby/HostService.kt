package com.yogistudio.molkkyboard.core.nearby

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service de premier plan de l'hôte (type `connectedDevice`) : la partie reste joignable écran éteint. Sa
 * notification dit combien d'invités sont connectés et propose « Arrêter le partage ».
 */
@AndroidEntryPoint
class HostService : Service() {
    @Inject
    lateinit var nearby: NearbyManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            nearby.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        val guests = intent?.getIntExtra(EXTRA_PHONES, 0) ?: 0
        val type =
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(this, guests), type)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "partie-a-cote"
        private const val NOTIFICATION_ID = 4_242
        private const val ACTION_STOP = "com.yogistudio.molkkyboard.nearby.STOP"
        private const val EXTRA_PHONES = "phones"

        fun start(context: Context): Intent = Intent(context, HostService::class.java)

        /** Met à jour le nombre d'invités connectés affiché (sans relancer le service s'il ne tourne pas). */
        fun refresh(context: Context, guests: Int) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.activeNotifications.any { it.id == NOTIFICATION_ID }) {
                manager.notify(NOTIFICATION_ID, notification(context, guests))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HostService::class.java))
        }

        private fun notification(context: Context, guests: Int): Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.nearby_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            val stop =
                PendingIntent.getService(
                    context,
                    0,
                    Intent(context, HostService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            return NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle(context.getString(R.string.nearby_notification_title))
                .setContentText(
                    context.resources.getQuantityString(R.plurals.nearby_notification_phones, guests, guests),
                ).setOngoing(true)
                .setOnlyAlertOnce(true)
                .apply {
                    open?.let {
                        setContentIntent(PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_IMMUTABLE))
                    }
                }.addAction(0, context.getString(R.string.nearby_notification_stop), stop)
                .build()
        }
    }
}
