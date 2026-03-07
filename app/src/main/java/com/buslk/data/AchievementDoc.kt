package com.buslk.data

data class AchievementDoc(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val requiredTotalTrips: Int = 0,
    val requiredReports: Int = 0
)

data class UserAchievementDoc(
    val achievementId: String = "",
    val unlockedAt: com.google.firebase.Timestamp? = null
)
