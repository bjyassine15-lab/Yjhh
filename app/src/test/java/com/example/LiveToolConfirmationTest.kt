package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.VoiceService
import com.example.ai.auth.DevelopmentGeminiAuthProvider
import com.example.ai.live.GeminiLiveService
import com.example.ai.tools.ToolExecutor
import com.example.data.local.AppDatabase
import com.example.data.repository.DailyPlannerRepository
import com.example.data.repository.FrenchWordRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.StoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveToolConfirmationTest {

    private lateinit var context: Context
    private lateinit var fakeExecutor: FakeToolExecutor
    private lateinit var liveService: GeminiLiveService

    class FakeToolExecutor(context: Context) : ToolExecutor(
        profileRepo = com.example.data.repository.ProfileRepository(AppDatabase.getInstance(context).profileDao()),
        memoryRepo = com.example.data.repository.MemoryRepository(AppDatabase.getInstance(context).memoryDao()),
        storyRepo = com.example.data.repository.StoryRepository(AppDatabase.getInstance(context).storyDao()),
        plannerRepo = com.example.data.repository.DailyPlannerRepository(AppDatabase.getInstance(context).dailyTaskDao()),
        frenchRepo = com.example.data.repository.FrenchWordRepository(AppDatabase.getInstance(context).frenchWordDao())
    ) {
        val executedTools = mutableListOf<Pair<String, Map<String, Any?>>>()

        override suspend fun executeTool(toolName: String, arguments: Map<String, Any?>): String {
            executedTools.add(toolName to arguments)
            return "Fake success result for $toolName"
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeExecutor = FakeToolExecutor(context)
        liveService = GeminiLiveService(
            context = context,
            fallbackVoiceService = VoiceService(context),
            authProvider = DevelopmentGeminiAuthProvider(),
            toolExecutor = fakeExecutor
        )
    }

    @Test
    fun `1 READ_TOOL executes automatically`() = runBlocking {
        val callId = "call_read_001"
        val functionName = "get_mother_profile"
        val arguments = emptyMap<String, Any?>()

        liveService.handleFunctionCall(callId, functionName, arguments)

        // Tool executes directly
        assertEquals(1, fakeExecutor.executedTools.size)
        assertEquals("get_mother_profile", fakeExecutor.executedTools[0].first)

        // No confirmation needed
        assertNull(liveService.pendingToolConfirmation.value)

        // Result returned to Gemini Live
        assertNotNull(liveService.lastSentToolResult)
        assertEquals(callId, liveService.lastSentToolResult?.callId)
        assertEquals(functionName, liveService.lastSentToolResult?.functionName)
        assertEquals("Fake success result for get_mother_profile", liveService.lastSentToolResult?.result)
    }

    @Test
    fun `2 SAFE_WRITE follows existing policy`() = runBlocking {
        val callId = "call_safe_002"
        val functionName = "save_memory"
        val arguments = mapOf<String, Any?>("content" to "أمي تفضل الشاي بالنعناع", "category" to "PREFERENCE")

        liveService.handleFunctionCall(callId, functionName, arguments)

        // Tool executes directly per safe write policy
        assertEquals(1, fakeExecutor.executedTools.size)
        assertEquals("save_memory", fakeExecutor.executedTools[0].first)

        // No pending confirmation
        assertNull(liveService.pendingToolConfirmation.value)

        // Result returned
        assertEquals("Fake success result for save_memory", liveService.lastSentToolResult?.result)
    }

    @Test
    fun `3 SENSITIVE_WRITE pauses for confirmation`() = runBlocking {
        val callId = "call_sensitive_003"
        val functionName = "delete_memory"
        val arguments = mapOf<String, Any?>("id" to 42)

        liveService.handleFunctionCall(callId, functionName, arguments)

        // Tool MUST NOT execute immediately!
        assertTrue(fakeExecutor.executedTools.isEmpty())

        // Confirmation must be pending
        val pending = liveService.pendingToolConfirmation.value
        assertNotNull(pending)
        assertEquals(callId, pending?.callId)
        assertEquals(functionName, pending?.functionName)
        assertEquals(arguments, pending?.arguments)

        // Result has not been sent yet
        assertNull(liveService.lastSentToolResult)
    }

    @Test
    fun `4 Reject does not execute the tool`() = runBlocking {
        val callId = "call_reject_004"
        val functionName = "delete_memory"
        val arguments = mapOf<String, Any?>("id" to 100)

        liveService.handleFunctionCall(callId, functionName, arguments)
        assertEquals(0, fakeExecutor.executedTools.size)
        assertNotNull(liveService.pendingToolConfirmation.value)

        // User rejects the tool action
        liveService.rejectLiveTool(callId)

        // Tool is NOT executed
        assertEquals(0, fakeExecutor.executedTools.size)

        // Pending confirmation is cleared
        assertNull(liveService.pendingToolConfirmation.value)

        // Informative rejection result is returned to Gemini Live
        assertNotNull(liveService.lastSentToolResult)
        assertEquals(callId, liveService.lastSentToolResult?.callId)
        assertEquals(functionName, liveService.lastSentToolResult?.functionName)
        assertTrue(liveService.lastSentToolResult?.result?.contains("رفض") == true)
    }

    @Test
    fun `5 Confirm executes exactly once`() = runBlocking {
        val callId = "call_confirm_005"
        val functionName = "save_health_note"
        val arguments = mapOf<String, Any?>("note" to "ضغط الدم 12/8")

        liveService.handleFunctionCall(callId, functionName, arguments)
        assertEquals(0, fakeExecutor.executedTools.size)

        // First confirm execution
        val result1 = liveService.confirmLiveTool(callId)
        assertEquals("Fake success result for save_health_note", result1)
        assertEquals(1, fakeExecutor.executedTools.size)
        assertEquals("save_health_note", fakeExecutor.executedTools[0].first)

        // Duplicate confirm call with same callId MUST NOT execute again
        val result2 = liveService.confirmLiveTool(callId)
        assertNull(result2)
        assertEquals(1, fakeExecutor.executedTools.size)
    }

    @Test
    fun `6 Original arguments are preserved`() = runBlocking {
        val callId = "call_args_006"
        val functionName = "add_daily_task"
        val originalArgs = mapOf<String, Any?>(
            "title" to "موعد مع طبيب العيون",
            "timeHint" to "16:00",
            "category" to "APPOINTMENT"
        )

        liveService.handleFunctionCall(callId, functionName, originalArgs)

        // Sensitive appointment task triggers confirmation
        val pending = liveService.pendingToolConfirmation.value
        assertNotNull(pending)
        assertEquals(originalArgs, pending?.arguments)
        assertEquals("موعد مع طبيب العيون", pending?.arguments?.get("title"))
        assertEquals("16:00", pending?.arguments?.get("timeHint"))
        assertEquals("APPOINTMENT", pending?.arguments?.get("category"))

        // Confirm
        liveService.confirmLiveTool(callId)

        // Executed tool received the exact original arguments
        assertEquals(1, fakeExecutor.executedTools.size)
        assertEquals(originalArgs, fakeExecutor.executedTools[0].second)
    }

    @Test
    fun `7 Tool result is returned to Gemini Live`() = runBlocking {
        val callId = "call_result_007"
        val functionName = "save_health_note"
        val arguments = mapOf<String, Any?>("note" to "أمي تشعر بالراحة والحمد لله")

        liveService.handleFunctionCall(callId, functionName, arguments)
        liveService.confirmLiveTool(callId)

        assertNotNull(liveService.lastSentToolResult)
        assertEquals(callId, liveService.lastSentToolResult?.callId)
        assertEquals(functionName, liveService.lastSentToolResult?.functionName)
        assertEquals("Fake success result for save_health_note", liveService.lastSentToolResult?.result)
    }
}
