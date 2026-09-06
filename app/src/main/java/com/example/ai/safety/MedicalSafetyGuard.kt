package com.example.ai.safety

/**
 * MedicalSafetyGuard for Rafiqah V2.1.
 * Evaluates both user input queries and assistant responses to guarantee:
 * 1. Immediate detection of urgent symptoms (Red Flags) directing to Emergency 190 (SAMU).
 * 2. Gentle triage of routine health queries with clear guidance to consult a physician.
 * 3. Prevention of autonomous AI diagnosis, prescription of drugs, or medication dosage recommendations.
 * 4. Ensuring all recorded health facts remain strictly user-reported.
 */
class MedicalSafetyGuard {

    enum class HealthRiskLevel {
        NORMAL,
        MEDICAL_REVIEW,
        URGENT
    }

    data class SafetyEvaluation(
        val riskLevel: HealthRiskLevel,
        val detectedKeywords: List<String>,
        val requiresEmergencyAdvice: Boolean,
        val emergencyAdviceMessage: String? = null,
        val recommendedDisclaimer: String? = null
    )

    private val urgentRedFlags = listOf(
        "وجيعة قوية في صدري", "ألم حاد في الصدر", "صدري يوجع برشة", "ألم في صدري", "وجيعة في صدري",
        "ضيق شديد في التنفس", "ما عادش نجم نتنفس", "خنقة قوية", "صعوبة في التنفس",
        "شلل", "فمي عوج", "عيني طاحت", "دوخة شديدة ومفاجئة", "فقدان وعي", "غيبوبة",
        "نزيف حاد", "نزيف قوي", "سكتة"
    )

    private val reviewSymptoms = listOf(
        "راسي يوجع", "صداع", "تونسيو طالعة", "ضغط دم", "سخانة", "حمى",
        "دوخة خفيفة", "تعب", "فشلة", "نومي مقلق", "ما رقدتش", "وجيعة في المفاصل",
        "ركبتي توجع", "معدتي", "كرشي توجع", "غثيان"
    )

    private val forbiddenPrescriptionWords = listOf(
        "أنصحك بأخذ دواء", "خذي دواء", "اشربي دواء", "جرعة 500", "جرعة 1000",
        "أشخص حالتك", "التشخيص المؤكد هو", "أنت مصابة بمرض"
    )

    /**
     * Evaluates incoming user input for medical risks.
     */
    fun evaluateInput(userUtterance: String): SafetyEvaluation {
        val lower = userUtterance.lowercase()

        // 1. Check Red Flags (Urgent)
        val matchedUrgent = urgentRedFlags.filter { lower.contains(it) }
        if (matchedUrgent.isNotEmpty()) {
            return SafetyEvaluation(
                riskLevel = HealthRiskLevel.URGENT,
                detectedKeywords = matchedUrgent,
                requiresEmergencyAdvice = true,
                emergencyAdviceMessage = "يا أمي الغالية، هذه علامة تستوجب فحصاً طبياً عاجلاً وبدون تأخير! بربي اتصلي فوراً بالإسعاف على الرقم 190 (SAMU) أو توجهي لأقرب قسم استعجالي، وربي يلطف بيك ويحفظك 🚨"
            )
        }

        // 2. Check routine medical review symptoms
        val matchedReview = reviewSymptoms.filter { lower.contains(it) }
        if (matchedReview.isNotEmpty()) {
            return SafetyEvaluation(
                riskLevel = HealthRiskLevel.MEDICAL_REVIEW,
                detectedKeywords = matchedReview,
                requiresEmergencyAdvice = false,
                recommendedDisclaimer = "هذه مجرد نصيحة رفيقة للاستئناس والراحة، ولا تعوض استشارة طبيبك المباشر."
            )
        }

        return SafetyEvaluation(
            riskLevel = HealthRiskLevel.NORMAL,
            detectedKeywords = emptyList(),
            requiresEmergencyAdvice = false
        )
    }

    /**
     * Post-processes AI generated response to ensure safety guidelines are enforced.
     */
    fun sanitizeOutput(rawAiReply: String, inputEvaluation: SafetyEvaluation): String {
        // If urgent, prioritize the immediate emergency alert
        if (inputEvaluation.riskLevel == HealthRiskLevel.URGENT) {
            val emergency = inputEvaluation.emergencyAdviceMessage
                ?: "يا أمي الحبيبة، أنصحك بالاتصال فوراً بالإسعاف (190) أو مراجعة أقرب طبيب حالاً للسلامة."
            return if (!rawAiReply.contains("190")) {
                "$emergency\n\n$rawAiReply"
            } else {
                rawAiReply
            }
        }

        var sanitized = rawAiReply

        // Remove dangerous direct prescriptive declarations
        for (forbidden in forbiddenPrescriptionWords) {
            if (sanitized.contains(forbidden)) {
                sanitized = sanitized.replace(forbidden, "يستحسن أن تسألي طبيبك بخصوص")
            }
        }

        // Ensure doctor consultation is advised if symptoms were discussed
        if (inputEvaluation.riskLevel == HealthRiskLevel.MEDICAL_REVIEW && !sanitized.contains("طبيب") && !sanitized.contains("استشارة")) {
            sanitized += "\n\n(تذكير لطيف يا أمي: إذا تواصلت عليك هذه الأعراض، من الأفضل دائماً استشارة طبيبك الخاص للاطمئنان الكامل 🌷)"
        }

        return sanitized
    }
}
