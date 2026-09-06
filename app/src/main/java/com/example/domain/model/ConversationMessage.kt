package com.example.domain.model

enum class MessageSender {
    USER,
    RAFIQAH
}

data class ConversationMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val spokenDialectText: String = text,
    val timestamp: Long = System.currentTimeMillis(),
    val isPlayingAudio: Boolean = false
)
