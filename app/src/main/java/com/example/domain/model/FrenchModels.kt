package com.example.domain.model

data class FrenchWordItem(
    val id: Int,
    val frenchWord: String,
    val arabicPhonetics: String,
    val arabicMeaning: String,
    val tunisianEverydayContext: String,
    val medicalContext: String,
    val exampleDailySentence: String,
    val exampleMedicalSentence: String,
    val interactivePrompt: String,
    val isMastered: Boolean = false
)
