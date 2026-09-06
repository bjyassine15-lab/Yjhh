package com.example.ai

/**
 * High-fidelity Mock AI Service for Rafiqah V2.
 * Serves as an offline-first engine, automated testing backbone,
 * and reliable fallback when network is absent or API keys are not supplied.
 * Fully satisfies the 7 core conversational scenarios specified in V2 requirements.
 */
class MockAIService : AIService {

    override suspend fun sendVoiceMessage(
        userSpeech: String,
        userProfileSummary: String,
        recentMemories: List<String>
    ): AIResponse {
        val normalized = userSpeech.trim().lowercase()

        // 1. Safety Emergency Check (Red Flags)
        if (normalized.contains("وجيعة قوية في صدري") ||
            normalized.contains("ألم في الصدر") ||
            normalized.contains("ضيق شديد في التنفس") ||
            normalized.contains("دوخة قوية وما نراش") ||
            normalized.contains("فقدت الوعي")
        ) {
            return AIResponse(
                replyText = "يا أمي ربي يحفظك، الوجيعة هاذي تستوجب رؤية طبيب فوراً أو الاتصال بالإسعاف على الرقم 190. ما تقعديش وحدك وكلمي شكون قريب منك توا.",
                spokenDialectText = "يا أمي الغالية، ربي يشفيك، هاذي علامة تستحق تشوفي طبيب حالا أو تتصلي بالإسعاف في تونس 190. ارتاحي وما تبذلي حتى مجهود.",
                isUrgentMedicalNotice = true,
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "شكت أمي من عارض صحي حاد يستوجب متابعة طبية عاجلة.",
                        category = "HEALTH",
                        importance = 5
                    )
                )
            )
        }

        // Scenario 1: "شنوة الخلية؟" -> Start from scratch with a basic analogy
        if (normalized.contains("شنوة الخلية") || normalized.contains("شنيا الخلية") || normalized.contains("ما هي الخلية") || (normalized.contains("فهمتني") && normalized.contains("خلية"))) {
            return AIResponse(
                replyText = "الخلية يا أمي هي كيما الياجورة في الحيط. شفتي الدار كيفاش مبنية من آلاف الياجورات؟ جسمنا الكل مبني من مليارات الخلايا الحية الصغيرة برشا. كل خلية تاكل وتتنفس وتخدم باش تحافظ على صحتك وقوتك 🌷",
                spokenDialectText = "الخلية يا أمي كيما الياجورة في الحيط، لكنها ياجورة حية تتنفس وتخدم باش تحميك وتخلي بدنك قوي.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "بدأت أمي تعلم مفهوم الخلية من التشبيه الأساسي بالياجورة الحية.",
                        category = "LEARNING",
                        importance = 3
                    )
                ),
                suggestedFollowUp = "تحبي نشوفو الباب اللي يحمي الخلية وهو الغشاء الخلوي؟"
            )
        }

        // Scenario 2: "ما فهمتش" -> Empathetic re-explanation with everyday analogy
        if (normalized.contains("ما فهمتش") || normalized.contains("صعيبة") || normalized.contains("ما توضحتش") || normalized.contains("عاودلي")) {
            return AIResponse(
                replyText = "يظهرلي الجزء هذا ما توضّحش مليح يا أمي. لا تقلقي روحك أبداً، خليني نفسروهالك بطريقة أخرى وناخذو مثال ساهل من الدار 🌸\nتخيلي الخلية كيما الدار التونسية: عندها باب خارجي هو 'الغشاء'، وصالة داخلية هي 'السيتوبلازم'، وبيت المونة والخزينة اللي فيها أسرار الدار هي 'النواة'. هكا واضحة أحسن؟",
                spokenDialectText = "يظهرلي الجزء هذا ما توضّحش مليح يا أمي، خليني نفسروهالك بطريقة أخرى. تخيلي الخلية كيما دارنا، عندها باب وجدران وغرفة رئيسية فيها كل الأسرار.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "احتاجت أمي لإعادة شرح الخلية بأسلوب الدار التونسية وتمت المراجعة بنجاح.",
                        category = "LEARNING",
                        importance = 4
                    )
                ),
                suggestedFollowUp = "شنوة رايك، توضحت الفكرة توا؟"
            )
        }

        // Scenario 3: "أنا فهمت" -> Encouraging validation check
        if (normalized.contains("أنا فهمت") || normalized.contains("فهمتها") || normalized.contains("واضحة عايشك") || (normalized.contains("فهمت") && normalized.contains("مليح"))) {
            return AIResponse(
                replyText = "ما شاء الله عليك يا أمي! ربي ينور عقلك ويزيدك من فضله 🌷 باش نثبتوا المعلومة ونفرحوا بيها: تتذكري شكون في الخلية اللي يتحكم ويحفظ أسرارها كيما إدارة الدار؟ هي النواة.",
                spokenDialectText = "ما شاء الله عليك يا أمي، مخك نير وفهمك ممتاز. هكا سجلت إنك أتقنتي المفهوم وننجموا نتقدموا للخطوة الجاية.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "أتقنت أمي مفهوم الخلية وأبدت استيعاباً كاملاً وارتياحاً.",
                        category = "LEARNING",
                        importance = 4
                    )
                ),
                suggestedFollowUp = "نفتحو الفصل الجديد في رواية سارة؟"
            )
        }

        // Scenario 4: "اليوم عندي موعد مع الطبيب" -> Natural confirmation with planner integration
        if ((normalized.contains("موعد") || normalized.contains("rendez-vous")) && (normalized.contains("طبيب") || normalized.contains("سبيطار") || normalized.contains("كلينيك") || normalized.contains("اليوم") || normalized.contains("غدوة"))) {
            return AIResponse(
                replyText = "ربي يعطيك صحة البدن يا أمي الحبيبة، وإن شاء الله الموعد يطمن قلبك 🩺.\nتحبي نسجل الموعد هذا في نهارك باش نذكّرك بيه في وقته وما تنسيش؟ إذا تحبي قوليلي وقتاش بالضبط.",
                spokenDialectText = "ربي يطمنك يا أمي الغالية، تحبي نسجل الموعد هذا في نهارك باش نذكّرك بيه في وقته وما تنسيش؟",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "أمي عندها موعد متابعة مع الطبيب، ويستحسن تذكيرها به وتجهيز الأسئلة.",
                        category = "HEALTH",
                        importance = 4
                    )
                ),
                suggestedFollowUp = "تحبي نسجلوه الساعة 10:00 صباحاً؟"
            )
        }

        // Scenario 5: User asks to continue the story -> References current chapter & Story Bible
        if (normalized.contains("قصة") || normalized.contains("رواية") || normalized.contains("سارة") || normalized.contains("وين وصلنا") || normalized.contains("نكمل")) {
            return AIResponse(
                replyText = "حاضر من عيوني يا أمي 📖. في رواية 'سارة... والطريق إلى الطب'، سارة كانت واقفة قدام المجهر في كلية الطب في تونس، وشافت كيفاش الخلية تخدم كأنها عايلة متضامنة في حومة سيدي بوسعيد. تحبي نكملوا كيفاش سارة شرحت هذا لأستاذها الدكتور توفيق؟",
                spokenDialectText = "من عيوني يا أمي، سارة اليوم في الكلية فرحانة برشا باكتشافها، ومتحمسة باش تشاركنا أسرار جديدة في جسم الإنسان.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "أمي تتابع بشغف أحداث الفصل الحالي من رواية سارة.",
                        category = "STORY",
                        importance = 3
                    )
                ),
                suggestedFollowUp = "نسمعو الفصل بصوت هادي؟"
            )
        }

        // Scenario 6: Health inquiry -> Empathetic health companion, no prescribing, advice on lifestyle
        if (normalized.contains("تونسيو") || normalized.contains("ضغط") || normalized.contains("دواء") || normalized.contains("راسي") || normalized.contains("قلب") || normalized.contains("نوم")) {
            return AIResponse(
                replyText = "سلامتك يا أمي الغالية وطهور إن شاء الله 🌸. التونسيو وصحة القلب ديما يعتمدو على راحة البال، شرب كويسات ماء كافية، وتقليل الملح في الماكلة التونسية. تذكري دواءك في وقته كما وصفه طبيبك، وإذا حسيتي بأي دوخة أو تعب مستمر، ما تتردديش في استشارة طبيبك في أقرب وقت.",
                spokenDialectText = "سلامة روحك يا أمي، ديما ارتاحي ونقصي الملح وخوذي دواءك في وقته. وربي يمتعك بالصحة والعافية.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "سألت أمي عن التونسيو وصحة الدورة الدموية، وتم تذكيرها بشرب الماء والالتزام بنصائح الطبيب.",
                        category = "HEALTH",
                        importance = 4
                    )
                ),
                suggestedFollowUp = "قستي التونسيو اليوم الصباح؟"
            )
        }

        // Scenario 7: "تتذكر الكلمة الفرنسية اللي قريتها البارح؟" -> Recalls French memory
        if ((normalized.contains("تتذكر") || normalized.contains("تتفكري")) && (normalized.contains("فرنسية") || normalized.contains("كلمة") || normalized.contains("بارح") || normalized.contains("قريتها"))) {
            return AIResponse(
                replyText = "أي نعم يا أمي، بالطبيعة نتفكر! 🌷 الكلمة اللي شفناها هي 'Rendez-vous' ونطقها [ راندي-فو ]. في تونس نقولو: 'عندي رانديفو عند الطبيب'، ومعناها موعد محدد ومضبوط. تتذكري كيفاش نطقناها مع بعضنا؟",
                spokenDialectText = "أي نعم أمي الغالية، نتفكر كلمة رانديفو، يعني موعد عند الطبيب أو في الإدارة. ما شاء الله عليك ذاكرتك ديما حية.",
                extractedMemories = listOf(
                    ExtractedMemoryCandidate(
                        content = "استرجعت أمي بنجاح كلمة Rendez-vous وطبقتها في سياق الحديث.",
                        category = "FRENCH",
                        importance = 3
                    )
                ),
                suggestedFollowUp = "تحبي نشوفو كلمة Ordonnance (أوردونونس) اليوم؟"
            )
        }

        // Default Warm Response
        return AIResponse(
            replyText = "على سلامتك يا أمي الحبيبة، نهارك مبروك ومزين بالياسمين 🌷. أنا رفيقتك ومعاك في كل وقت، تحبي نحكيو على صحتك، ولا نواصلوا قصة سارة في الطب، ولا نكتشفوا كلمة جديدة بالفرنسية؟",
            spokenDialectText = "على سلامتك أمي الغالية، نهارك زين. أنا هنا ديما باش نونسك ونفرحك.",
            extractedMemories = listOf(
                ExtractedMemoryCandidate(
                    content = "تواصلت أمي في محادثة عامة دافئة.",
                    category = "PERSONAL",
                    importance = 2
                )
            ),
            suggestedFollowUp = "تفضلي اسأليني أي سؤال يخطر ببالك."
        )
    }

    override suspend fun getProgressiveConceptExplanation(conceptKey: String, level: Int): String {
        return when (conceptKey) {
            "CELL" -> when (level) {
                1 -> "المستوى 1 (التشبيه البسيط): الخلية كيما الياجورة في الحيط. جسمنا الكل مبني من مليارات الياجورات الحية الصغيرة."
                2 -> "المستوى 2 (من الواقع التونسي): كيما الدار فيها كوجينة وصالة وباب محروس، الخلية فيها أجزاء كل واحد عندو خدمتو."
                3 -> "المستوى 3 (الفكرة العلمية): الخلية (Cellule) هي أصغر وحدة حية قادرة تتنفس، تاكل، وتتوالد في الكائن الحي."
                4 -> "المستوى 4 (الأعضاء الداخلية): داخلها فما 'النواة' اللي تحافظ على الـ DNA، و'الميتوكوندريا' اللي تصنع الطاقة، و'السيتوبلازم' اللي تعوم فيه الأعضاء."
                else -> "المستوى 5 (التطبيق الطبي): وقت اللي نمرضو ولا ناخذو دواء، راهو الدواء يمشي مباشرة للخلية باش يعاونها تحمي روحها وترجع تخدم مريقلة."
            }
            "HEART" -> when (level) {
                1 -> "المستوى 1: القلب كيما البومبة (المضخة) اللي تدفع الماء في الجعبة، يدفع الدم في كل عروق البدن."
                2 -> "المستوى 2: وقت اللي نحطو يدنا على صدرنا ونحسو الدقات، هاذيك عضلات القلب تنقبض وتتوسع بلا راحة."
                3 -> "المستوى 3: القلب فيه أربعة بيوت: زوز أذينين وزوز بطينين، مع صمامات ذكية تمنع الدم يرجع لتالي."
                4 -> "المستوى 4: الدم اللي يخرج من القلب يكون غني بالأكسجين والنقاء، وكي يدور في البدن يرجع باش يتصفى في الرئتين."
                else -> "المستوى 5: المشي الخفيف يومياً وشرب الماء يخلي عضلة القلب قوية ومرتاحة، ويبعد عليها التعب والجلطات."
            }
            else -> "مفهوم مبسط: كل عضو في جسمنا هو آية في الدقة، يخدم بتناغم ومحبة."
        }
    }

    override suspend fun generateDailyGreeting(
        motherName: String,
        timeOfDay: String,
        lastLessonTitle: String
    ): String {
        return "صباح الخير والياسمين يا $motherName 🌷. اليوم عندنا حكاية صغيرة وممتعة على $lastLessonTitle، وبعدها نطمنوا على صحتك ويومك."
    }

    override suspend fun evaluateAnswer(
        questionKey: String,
        selectedOptionIndex: Int
    ): ComprehensionResult {
        return if (selectedOptionIndex == 1) {
            ComprehensionResult(
                isCorrect = true,
                feedbackTunisian = "يعطيك الصحة يا أمي! جوابك صحيح مائة بالمائة. الخلية فعلاً فيها أجزاء تخدم مع بعضها كيما المعمل المنظم.",
                encouragementMessage = "ما شاء الله عليك، مخك حاضر وفهمك ممتاز!"
            )
        } else {
            ComprehensionResult(
                isCorrect = false,
                feedbackTunisian = "قريبة برشا يا أمي! لكن تذكري أن الخلية ماهيش كتلة وحدة مصمتة، بل هي مدينة صغيرة فيها أجزاء حية تتعاون مع بعضها.",
                encouragementMessage = "خطوة باهية، هكا نتعلمو ونثبتو المعلومة بالشوية بالشوية!"
            )
        }
    }
}
