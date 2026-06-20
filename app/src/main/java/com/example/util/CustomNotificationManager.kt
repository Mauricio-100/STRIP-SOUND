package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

class CustomNotificationManager(private val context: Context) {
    private val REPORT_CHANNEL_ID = "report_channel"
    private val ACTIVITY_CHANNEL_ID = "activity_channel"

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reportChannel = NotificationChannel(
                REPORT_CHANNEL_ID, "Signalements", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications pour signaler les abus ou les utilisateurs" }
            notificationManager.createNotificationChannel(reportChannel)

            val activityChannel = NotificationChannel(
                ACTIVITY_CHANNEL_ID, "Activités", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications pour les activités de votre compte (likes, commentaires, etc.)" }
            notificationManager.createNotificationChannel(activityChannel)
        }
    }

    fun showReportNotification(username: String) {
        val builder = NotificationCompat.Builder(context, REPORT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Signalement pris en compte")
            .setContentText("L'utilisateur $username a été signalé à l'équipe de modération.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun showActivityNotification(title: String, message: String) {
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.strip_app_logo_1781423781920)
        
        val builder = NotificationCompat.Builder(context, ACTIVITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
