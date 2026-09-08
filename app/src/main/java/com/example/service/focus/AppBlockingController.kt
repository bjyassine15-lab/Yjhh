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
    NOT_AVAILABLE,                   // Device doesn't support or feature is unavailable
    AVAILABLE_WITH_SPECIAL_ACCESS,   // Supported, but special permission (Usage Access) needed
    ACTIVE,                          // Policy actively blocking/monitoring chosen apps during Focus
    INACTIVE                         // Ready to be activated when Focus starts
}

data class BlockingStatus(
    val state: BlockingState,
    val hasUsageAccess: Boolean,
    val activeBlockedAppsCount: Int,
    val statusMessage: String
)

/**
 * Real Android AppBlockingController adhering to Android platform security and policies.
 * Uses UsageStatsManager & AppOpsManager to verify access, manages blocked apps list,
 * and activates/deactivates the focus blocking policy gracefully.
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
            isFocusPolicyActive && hasPermission -> BlockingState.ACTIVE
            isFocusPolicyActive && !hasPermission -> BlockingState.AVAILABLE_WITH_SPECIAL_ACCESS
            !hasPermission -> BlockingState.AVAILABLE_WITH_SPECIAL_ACCESS
            blockedApps.isEmpty() -> BlockingState.INACTIVE
            else -> BlockingState.INACTIVE
        }

        val message = when (state) {
            BlockingState.ACTIVE -> "وضع التركيز نشط: يتم مراقبة ${blockedApps.size} تطبيق للحد من التشتت."
            BlockingState.AVAILABLE_WITH_SPECIAL_ACCESS -> "لحجب التطبيقات المشتتة، يتطلب النظام تفعيل إذن الوصول للاستخدام (Usage Access) من الإعدادات."
            BlockingState.INACTIVE -> if (blockedApps.isEmpty()) "لم يتم تحديد تطبيقات لحجبها أثناء التركيز." else "جاهز للتفعيل عند بدء الجلسة (${blockedApps.size} تطبيق محدد)."
            BlockingState.NOT_AVAILABLE -> "ميزة الحجب المتقدم غير مدعومة على هذا النظام."
        }

        return BlockingStatus(
            state = state,
            hasUsageAccess = hasPermission,
            activeBlockedAppsCount = blockedApps.size,
            statusMessage = message
        )
    }
}
