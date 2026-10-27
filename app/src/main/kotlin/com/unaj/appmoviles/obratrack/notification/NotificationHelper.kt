package com.unaj.appmoviles.obratrack.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unaj.appmoviles.obratrack.MainActivity
import com.unaj.appmoviles.obratrack.R
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "obratrack_reminders"

    /** Contexto con el idioma elegido en la app; el de una notificación no lo recibe de ninguna Activity. */
    fun contextoLocalizado(context: Context, language: AppLanguage): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language.code))
        return context.createConfigurationContext(config)
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_canal_nombre),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_canal_descripcion)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        /** Si viene, tocar la notificación abre el detalle de esa obra. */
        obraId: String? = null
    ) {
        // Sin CLEAR_TASK: si la app está abierta, vuelve a la pantalla donde estaba
        // (con CLEAR_TASK se recreaba la Activity y se perdía, por ejemplo, un formulario a medio cargar).
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Solo el id, en el dato: además del request code, distingue el PendingIntent de cada obra.
            if (esObraIdValido(obraId)) {
                action = ACTION_ABRIR_OBRA
                data = Uri.parse(uriObra(obraId!!))
            }
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignorado si el usuario no autorizó POST_NOTIFICATIONS en Android 13+
        }
    }

    fun showDemoNotification(context: Context) {
        createNotificationChannels(context)
        showNotification(
            context = context,
            notificationId = 1001,
            title = context.getString(R.string.notif_demo_titulo),
            message = context.getString(R.string.notif_demo_mensaje)
        )
    }
}