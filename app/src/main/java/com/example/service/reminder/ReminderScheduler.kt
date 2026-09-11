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
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

data class ReminderScheduleResult(
    val success: Boolean,
    val notificationPermitted: Boolean,
    val isExact: Boolean,
    val feedbackMessage: String
)

/**
 * Real Android BroadcastReceiver for delivering scheduled reminders with exact destinations & recurrence.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير من رفيقة"
        val reminderId = intent.getLongExtra(EXTRA_ID, 0L)
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "GENERAL"
        val destination = intent.getStringExtra(EXTRA_DESTINATION) ?: mapCategoryToDestination(category)
        val isRecurring = intent.getBooleanExtra(EXTRA_IS_RECURRING, false)
        val recurrenceRule = intent.getStringExtra(EXTRA_RECURRENCE_RULE) ?: "DAILY"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val channelId = when (category) {
            "MEDICINE", "HEALTH" -> CHANNEL_HEALTH
            "READING", "LEARNING" -> CHANNEL_LEARNING
            "FOCUS" -> CHANNEL_FOCUS
            else -> CHANNEL_REMINDERS
        }

        // Tap intent leading directly to the specific screen destination
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_ID, reminderId)
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

        // Handle recurrence rescheduling (FIX 3 & FIX 17 - preserving original time-of-day without drift)
        if (isRecurring) {
            val scheduler = ReminderScheduler(context)
            val originalTrigger = intent.getLongExtra(EXTRA_ORIGINAL_TRIGGER, System.currentTimeMillis())
            val nextTrigger = calculateNextOccurrence(
                originalTriggerMillis = originalTrigger,
                recurrenceRule = recurrenceRule
            )
            if (nextTrigger > 0L) {
                scheduler.scheduleReminder(
                    id = reminderId,
                    title = title,
                    triggerMillis = nextTrigger,
                    category = category,
                    destination = destination,
                    isRecurring = true,
                    recurrenceRule = recurrenceRule,
                    originalTriggerMillis = originalTrigger
                )
            }
        }
    }

    private fun calculateNextOccurrence(
        originalTriggerMillis: Long,
        recurrenceRule: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val original = java.util.Calendar.getInstance().apply {
            timeInMillis = originalTriggerMillis
        }

        val next = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, original.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, original.get(java.util.Calendar.MINUTE))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        when (recurrenceRule.uppercase()) {
            "WEEKLY" -> {
                while (next.timeInMillis <= nowMillis) {
                    next.add(java.util.Calendar.DAY_OF_YEAR, 7)
                }
            }
            "DAILY" -> {
                while (next.timeInMillis <= nowMillis) {
                    next.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            else -> return 0L
        }

        return next.timeInMillis
    }

    private fun mapCategoryToDestination(category: String): String {
        return when (category) {
            "READING" -> "reading"
            "LEARNING", "FRENCH" -> "learning"
            "FOCUS" -> "focus"
            "HEALTH", "MEDICINE" -> "health"
            else -> "planner"
        }
    }

    companion object {
        const val EXTRA_ID = "com.example.reminder.EXTRA_ID"
        const val EXTRA_TITLE = "com.example.reminder.EXTRA_TITLE"
        const val EXTRA_CATEGORY = "com.example.reminder.EXTRA_CATEGORY"
        const val EXTRA_DESTINATION = "com.example.reminder.EXTRA_DESTINATION"
        const val EXTRA_IS_RECURRING = "com.example.reminder.EXTRA_IS_RECURRING"
        const val EXTRA_RECURRENCE_RULE = "com.example.reminder.EXTRA_RECURRENCE_RULE"
        const val EXTRA_ORIGINAL_TRIGGER = "com.example.reminder.EXTRA_ORIGINAL_TRIGGER"

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
 * Real AlarmManager Scheduler respecting Android version policies and permissions.
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canPostNotifications(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun scheduleReminder(
        id: Long,
        title: String,
        triggerMillis: Long,
        category: String = "GENERAL",
        destination: String? = null,
        isRecurring: Boolean = false,
        recurrenceRule: String? = null,
        originalTriggerMillis: Long = triggerMillis
    ): ReminderScheduleResult {
        if (alarmManager == null) {
            return ReminderScheduleResult(
                success = false,
                notificationPermitted = false,
                isExact = false,
                feedbackMessage = "خدمة التنبيهات غير متوفرة على هذا الجهاز."
            )
        }

        val notifPermitted = canPostNotifications()
        val exactPermitted = canScheduleExactAlarms()

        val dest = destination ?: when (category) {
            "READING" -> "reading"
            "LEARNING", "FRENCH" -> "learning"
            "FOCUS" -> "focus"
            "HEALTH", "MEDICINE" -> "health"
            else -> "planner"
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_CATEGORY, category)
            putExtra(ReminderReceiver.EXTRA_DESTINATION, dest)
            putExtra(ReminderReceiver.EXTRA_IS_RECURRING, isRecurring)
            putExtra(ReminderReceiver.EXTRA_RECURRENCE_RULE, recurrenceRule ?: "DAILY")
            putExtra(ReminderReceiver.EXTRA_ORIGINAL_TRIGGER, originalTriggerMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val isExactUsed: Boolean
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (exactPermitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    isExactUsed = true
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    isExactUsed = false
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                isExactUsed = true
            }

            val feedback = if (!notifPermitted) {
                "تمت جدولة التذكير بنجاح، لكن التذكيرات تحتاج تفعيل الإشعارات في إعدادات الهاتف حتى يصلك الصوت والتنبيه يا أمي 🌸."
            } else {
                "تمت جدولة التذكير بنجاح."
            }

            return ReminderScheduleResult(
                success = true,
                notificationPermitted = notifPermitted,
                isExact = isExactUsed,
                feedbackMessage = feedback
            )
        } catch (e: Exception) {
            return ReminderScheduleResult(
                success = false,
                notificationPermitted = notifPermitted,
                isExact = false,
                feedbackMessage = "تعذر ضبط التنبيه: ${e.message}"
            )
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
