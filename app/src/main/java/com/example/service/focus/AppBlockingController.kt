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
    MONITORING_ONLY, // Focus mode monitors usage & gives gentle alerts without claiming system app kill
    ACTIVE,         // Only if real device-owner or system-level blocking is available
    UNAVAILABLE     // System or platform does not support app blocking
}

enum class BlockingCapability {
    REAL_BLOCKING_AVAILABLE,
    MONITORING_ONLY,
    NEEDS_SPECIAL_ACCESS,
    UNSUPPORTED
}

enum class FocusBlockingMode {
    ACTIVE,         // Foreground monitoring active
    TIMER_ONLY      // Standard quiet timer mode
}

data class BlockingStatus(
    val state: BlockingState,
    val capability: BlockingCapability,
    val focusBlockingMode: FocusBlockingMode = FocusBlockingMode.TIMER_ONLY,
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
        val blockedApps = focusRepo.getActiveBlockedApps()
        val hasUsageAccess = checkUsageAccessPermission()
        isFocusPolicyActive = true

        val capability = if (!hasUsageAccess) {
            BlockingCapability.NEEDS_SPECIAL_ACCESS
        } else {
            // Standard Android Usage Access provides foreground observation, NOT force-kill blocking.
            BlockingCapability.MONITORING_ONLY
        }

        val state = if (!hasUsageAccess) {
            BlockingState.NOT_CONFIGURED
        } else {
            BlockingState.MONITORING_ONLY
        }

        val status = BlockingStatus(
            state = state,
            capability = capability,
            focusBlockingMode = if (hasUsageAccess) FocusBlockingMode.ACTIVE else FocusBlockingMode.TIMER_ONLY,
            hasUsageAccess = hasUsageAccess,
            activeBlockedAppsCount = blockedApps.size,
            statusMessage = if (hasUsageAccess) {
                "وضع التركيز يعمل بالمراقبة والتنبيه فقط."
            } else {
                "يجب تفعيل صلاحية الوصول إلى بيانات الاستخدام (Usage Access)."
            },
            explanationNote = "هذا الجهاز لا يمنح التطبيق صلاحية إغلاق أو منع التطبيقات الأخرى باستخدام Usage Access وحده."
        )

        _status.value = status
        return status
    }

    suspend fun deactivateFocusBlocking(): BlockingStatus {
        isFocusPolicyActive = false
        return refreshStatus()
    }

    private fun computeCurrentStatus(blockedApps: List<BlockedAppEntity>): BlockingStatus {
        val hasPermission = checkUsageAccessPermission()
        val capability = if (!hasPermission) {
            BlockingCapability.NEEDS_SPECIAL_ACCESS
        } else {
            BlockingCapability.MONITORING_ONLY
        }

        val state = when {
            !hasPermission -> BlockingState.NOT_CONFIGURED
            isFocusPolicyActive -> BlockingState.MONITORING_ONLY
            else -> BlockingState.MONITORING_ONLY
        }

        val mode = if (isFocusPolicyActive && hasPermission) {
            FocusBlockingMode.ACTIVE
        } else {
            FocusBlockingMode.TIMER_ONLY
        }

        val message = when {
            !hasPermission -> "يجب تفعيل صلاحية الوصول إلى بيانات الاستخدام (NOT_CONFIGURED)"
            isFocusPolicyActive -> "وضع التركيز والمتابعة نشط (${blockedApps.size} تطبيق تحت المراقبة)"
            blockedApps.isEmpty() -> "جاهز للمتابعة (لم يتم تحديد تطبيقات للمراقبة بعد)"
            else -> "جاهز للمتابعة مع ${blockedApps.size} تطبيق محدد"
        }

        val explanation = "وضع المؤقت والمراقبة: حساب الوقت الهادئ دون إغلاق التطبيقات قسراً لعدم توفر الصلاحية الإدارية على مستوى النظام."

        return BlockingStatus(
            state = state,
            capability = capability,
            focusBlockingMode = mode,
            hasUsageAccess = hasPermission,
            activeBlockedAppsCount = blockedApps.size,
            statusMessage = message,
            explanationNote = explanation
        )
    }
}
