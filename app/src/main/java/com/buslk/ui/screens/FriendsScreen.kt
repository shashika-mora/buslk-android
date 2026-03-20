package com.buslk.ui.screens

// --- Mock Data ---
data class FriendState(
    val name: String,
    val initials: String,
    val statusText: String,
    val isOnline: Boolean,
    val unreadCount: Int = 0,
    val isOnBus: Boolean = false
)

val mockOnlineFriends = listOf(
    FriendState("Nimal Perera", "NP", "Active now", true, 1),
    FriendState("Maya Rajapaksa", "MR", "Active 5m ago", true, 0)
)

val mockOtherBusesFriends = listOf(
    FriendState("Priya Silva", "PS", "On Route 138", true, 2, isOnBus = true),
    FriendState("Kamal Fernando", "KF", "On Route 138", true, 0, isOnBus = true),
    FriendState("Saman De Silva", "SD", "Active 10m ago", true, 0, isOnBus = true)
)