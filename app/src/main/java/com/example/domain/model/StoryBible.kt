package com.example.domain.model

data class StoryCharacter(
    val name: String,
    val role: String,
    val descriptionTunisian: String
)

data class StoryBible(
    val novelTitle: String = "سارة... والطريق إلى الطب",
    val setting: String = "تونس العاصمة، بين كلية الطب وحومة سيدي بوسعيد، والحياة اليومية التونسية الهادئة",
    val characters: List<StoryCharacter> = listOf(
        StoryCharacter(
            name = "سارة",
            role = "البطلة - طالبة طب وباحثة",
            descriptionTunisian = "بنت ذكية، تحب أمها برشة، وتشرح أسرار جسم الإنسان بحب وبأمثلة من الدار والشارع التونسي"
        ),
        StoryCharacter(
            name = "الدكتور توفيق",
            role = "أستاذ الطب الحكيم",
            descriptionTunisian = "طبيب متقاعد يعلم سارة أن الطب حكمة وإنسانية ورحمة قبل أن يكون كتب ومصطلحات معقدة"
        ),
        StoryCharacter(
            name = "الخالة فاطمة",
            role = "الأم الحبيبة",
            descriptionTunisian = "سيدة تونسية حكيمة، كريمة القلب، تفهم بالقياس والأمثلة وتعتني بصحتها وصحة عائلتها"
        )
    ),
    val currentChapter: Int = 1,
    val previousEventsSummary: String = "في البداية، اكتشفت سارة بالمجهر عالماً مصغراً مدهشاً داخل نقطة ماء، وشبهت الخلية ببيت تونسي قديم تحميه جدرانه وتتحكم فيه غرفته الرئيسية (النواة).",
    val keyConceptsCovered: List<String> = listOf("BODY", "CELL", "CELL_MEMBRANE")
)
