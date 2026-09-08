package com.example.service.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

/**
 * Real Android BroadcastReceiver for delivering scheduled reminders.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير من رفيقة"
        val reminderId = intent.getLongExtra(EXTRA_ID, 0L)
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "GENERAL"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val channelId = when (category) {
            "MEDICINE", "HEALTH" -> CHANNEL_HEALTH
            "READING", "LEARNING" -> CHANNEL_LEARNING
            "FOCUS" -> CHANNEL_FOCUS
            else -> CHANNEL_REMINDERS
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("رفيقة 🌸")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }

    companion object {
        const val EXTRA_ID = "com.example.reminder.EXTRA_ID"
        const val EXTRA_TITLE = "com.example.reminder.EXTRA_TITLE"
        const val EXTRA_CATEGORY = "com.example.reminder.EXTRA_CATEGORY"

        const val CHANNEL_REMINDERS = "rafiqah_reminders_channel"
        const val CHANNEL_HEALTH = "rafiqah_health_channel"
        const val CHANNEL_LEARNING = "rafiqah_learning_channel"
        const val CHANNEL_FOCUS = "rafiqah_focus_channel"

        fun createChannels(notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channels = listOf(
                    NotificationChannel(CHANNEL_REMINDERS, "تذكيرات رفيقة اليومية", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "مواعيد وتذكيرات المهام اليومية والروتين"
                    },
                    NotificationChannel(CHANNEL_HEALTH, "الصحة والعادات الطيبة", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "تذكير شرب الماء والحركة وأوقات الراحة"
                    },
                    NotificationChannel(CHANNEL_LEARNING, "جلسات التعلم والقراءة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "أوقات القراءة اليومية والكلمات الفرنسية"
                    },
                    NotificationChannel(CHANNEL_FOCUS, "وضع التركيز", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "إشعارات جلسات التركيز المكتملة"
                    }
                )
                channels.forEach { notificationManager.createNotificationChannel(it) }
            }
        }
    }
}

/**
 * Real AlarmManager Scheduler respecting Android version policies.
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(id: Long, title: String, triggerMillis: Long, category: String = "GENERAL"): Boolean {
        if (alarmManager == null) return false
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_CATEGORY, category)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setAndAllowWhileIdle for battery-friendly, reliable delivery
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun cancelReminder(id: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
