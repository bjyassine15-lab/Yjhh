package com.example.service.focus

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BlockingState {
    NOT_CONFIGURED, // Special permission (Usage Access) has not been granted yet
    AVAILABLE,      // Usage access granted, ready to monitor/block chosen apps
    ACTIVE,         // Focus session is active and app blocking/monitoring policy is active
    UNAVAILABLE     // System or platform does not support app blocking
}

enum class FocusBlockingMode {
    ACTIVE,         // Real blocking / foreground monitoring active
    TIMER_ONLY      // Standard quiet timer mode; apps are not blocked forcefully
}

data class BlockingStatus(
    val state: BlockingState,
    val focusBlockingMode: FocusBlockingMode,
    val hasUsageAccess: Boolean,
    val activeBlockedAppsCount: Int,
    val statusMessage: String,
    val explanationNote: String
)

/**
 * Real Android AppBlockingController adhering to Android platform security and policies.
 * Uses UsageStatsManager & AppOpsManager to verify access, manages blocked apps list,
 * detects foreground applications, and provides honest UI states without fake blocking.
 */
class AppBlockingController(
    private val context: Context,
    private val focusRepo: FocusRepository
) {
    private val _status = MutableStateFlow(computeCurrentStatus(emptyList()))
    val status: StateFlow<BlockingStatus> = _status.asStateFlow()

    private var isFocusPolicyActive: Boolean = false

    fun checkUsageAccessPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Checks the currently foreground app using real Android UsageStatsManager
     */
    fun getCurrentlyForegroundApp(): String? {
        if (!checkUsageAccessPermission()) return null
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return null
            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(now - 15_000, now)
            var lastForegroundPkg: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPkg = event.packageName
                }
            }
            lastForegroundPkg
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshStatus(): BlockingStatus {
        val blockedApps = focusRepo.getActiveBlockedApps()
        val updated = computeCurrentStatus(blockedApps)
        _status.value = updated
        return updated
    }

    suspend fun activateFocusBlocking(): BlockingStatus {
        isFocusPolicyActive = true
        return refreshStatus()
    }

    suspend fun deactivateFocusBlocking(): BlockingStatus {
        isFocusPolicyActive = false
        return refreshStatus()
    }

    private fun computeCurrentStatus(blockedApps: List<BlockedAppEntity>): BlockingStatus {
        val hasPermission = checkUsageAccessPermission()
        val state = when {
            !hasPermission -> BlockingState.NOT_CONFIGURED
            isFocusPolicyActive -> BlockingState.ACTIVE
            else -> BlockingState.AVAILABLE
        }

        val mode = if (state == BlockingState.ACTIVE && hasPermission && blockedApps.isNotEmpty()) {
            FocusBlockingMode.ACTIVE
        } else {
            FocusBlockingMode.TIMER_ONLY
        }

        val message = when (state) {
            BlockingState.NOT_CONFIGURED -> "إذن الوصول للاستخدام غير مفعّل (NOT_CONFIGURED)"
            BlockingState.AVAILABLE -> if (blockedApps.isEmpty()) "جاهز للتفعيل (لم يتم تحديد تطبيقات لحجبها بعد)" else "جاهز للتفعيل مع ${blockedApps.size} تطبيق محدد"
            BlockingState.ACTIVE -> "حماية التركيز مفعلة (${blockedApps.size} تطبيق مراقب)"
            BlockingState.UNAVAILABLE -> "الحجب المتقدم غير متاح على هذا النظام"
        }

        val explanation = when (mode) {
            FocusBlockingMode.ACTIVE -> "وضع الحجب النشط: يتم رصد التطبيقات المشتتة وتنبيه الأم لحماية وقت التركيز."
            FocusBlockingMode.TIMER_ONLY -> "وضع المؤقت الهادئ (TIMER_ONLY): حساب الوقت دون إغلاق التطبيقات قسراً لعدم توفر الصلاحية الإدارية."
        }

        return BlockingStatus(
            state = state,
            focusBlockingMode = mode,
            hasUsageAccess = hasPermission,
            activeBlockedAppsCount = blockedApps.size,
            statusMessage = message,
            explanationNote = explanation
        )
    }
}
