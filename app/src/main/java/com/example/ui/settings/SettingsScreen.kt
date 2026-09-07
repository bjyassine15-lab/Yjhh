package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.auth.ConnectionTestResult
import com.example.ai.auth.GeminiConnectionStatus
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.TerracottaAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    connectionStatus: GeminiConnectionStatus,
    maskedApiKey: String?,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTestConnection: suspend () -> ConnectionTestResult,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var inputKeyText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultFeedback by remember { mutableStateOf<ConnectionTestResult?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات ⚙️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Section Header: AI Configuration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RafiqahRose.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = RafiqahRose,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الذكاء الاصطناعي — إعداد Gemini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ربط التطبيق بنموذج Google Gemini الحقيقي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connection Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionStatus) {
                        GeminiConnectionStatus.CONNECTED -> SageOlive.copy(alpha = 0.12f)
                        GeminiConnectionStatus.FAILED -> TerracottaAccent.copy(alpha = 0.12f)
                        GeminiConnectionStatus.CONFIGURED -> GoldAccent.copy(alpha = 0.12f)
                        GeminiConnectionStatus.TESTING -> MaterialTheme.colorScheme.surfaceVariant
                        GeminiConnectionStatus.NOT_CONFIGURED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_connection_status_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    val statusIcon = when (connectionStatus) {
                        GeminiConnectionStatus.CONNECTED -> Icons.Default.CloudDone
                        GeminiConnectionStatus.FAILED -> Icons.Default.CloudOff
                        GeminiConnectionStatus.CONFIGURED -> Icons.Default.CloudQueue
                        GeminiConnectionStatus.TESTING -> Icons.Default.NetworkCheck
                        GeminiConnectionStatus.NOT_CONFIGURED -> Icons.Default.Key
                    }
                    val statusColor = when (connectionStatus) {
                        GeminiConnectionStatus.CONNECTED -> SageOlive
                        GeminiConnectionStatus.FAILED -> TerracottaAccent
                        GeminiConnectionStatus.CONFIGURED -> GoldAccent
                        GeminiConnectionStatus.TESTING -> RafiqahRose
                        GeminiConnectionStatus.NOT_CONFIGURED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حالة الاتصال بـ Gemini:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = connectionStatus.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    if (connectionStatus == GeminiConnectionStatus.TESTING || isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = RafiqahRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Saved Key Info
            if (!maskedApiKey.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SageOlive,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المفتاح المحفوظ حالياً مشفر بأمان:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = maskedApiKey,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input Field: Gemini API Key
            OutlinedTextField(
                value = inputKeyText,
                onValueChange = { inputKeyText = it },
                label = { Text("Gemini API Key") },
                placeholder = { Text("أدخل المفتاح هنا (AIzaSy...)") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (inputKeyText.isNotBlank()) {
                            onSaveApiKey(inputKeyText.trim())
                            inputKeyText = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("تم حفظ مفتاح API بنجاح 🔒")
                            }
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.testTag("settings_toggle_visibility_button")
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "إخفاء المفتاح" else "إظهار المفتاح"
                        )
                    }
                },
                supportingText = {
                    Text(
                        text = "يمكنك الحصول على مفتاح مجاني من aistudio.google.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RafiqahRose,
                    focusedLabelColor = RafiqahRose
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_key_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Save, Test Connection, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val trimmed = inputKeyText.trim()
                        if (trimmed.isNotBlank()) {
                            onSaveApiKey(trimmed)
                            inputKeyText = ""
                            testResultFeedback = null
                            scope.launch {
                                snackbarHostState.showSnackbar("تم حفظ المفتاح بنجاح 🔒")
                            }
                        }
                    },
                    enabled = inputKeyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_save_key_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ المفتاح")
                }

                // Clear Button
                OutlinedButton(
                    onClick = {
                        onClearApiKey()
                        inputKeyText = ""
                        testResultFeedback = null
                        scope.launch {
                            snackbarHostState.showSnackbar("تم مسح المفتاح")
                        }
                    },
                    enabled = !maskedApiKey.isNullOrBlank() || inputKeyText.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("settings_clear_key_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Connection Button (Always accessible if a key exists or is being entered)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    // If user typed a key but didn't click save yet, save it first
                    if (inputKeyText.isNotBlank()) {
                        onSaveApiKey(inputKeyText.trim())
                        inputKeyText = ""
                    }
                    scope.launch {
                        isTestingConnection = true
                        testResultFeedback = null
                        val result = onTestConnection()
                        testResultFeedback = result
                        isTestingConnection = false
                        snackbarHostState.showSnackbar(result.message)
                    }
                },
                enabled = !isTestingConnection && (!maskedApiKey.isNullOrBlank() || inputKeyText.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = SageOlive),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_test_connection_button")
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الاتصال بـ Gemini...")
                } else {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختبار الاتصال بـ Gemini 🌐")
                }
            }

            // Test Result Feedback Banner
            AnimatedVisibility(visible = testResultFeedback != null) {
                testResultFeedback?.let { feedback ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (feedback.success) SageOlive.copy(alpha = 0.15f) else TerracottaAccent.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (feedback.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (feedback.success) SageOlive else TerracottaAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = feedback.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (feedback.success) SageOlive else TerracottaAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Security & Privacy Architecture Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = RafiqahRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أمان المفتاح والخصوصية 🛡️",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• يُخزن المفتاح محلياً على هاتفك فقط باستخدام AndroidX EncryptedSharedPreferences.\n" +
                                "• لا يتم إرسال المفتاح إلى أي جهة خارجية سوى خوادم Google Gemini الرسمية.\n" +
                                "• التطبيق مخصص للاستخدام الشخصي والتطوير.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
