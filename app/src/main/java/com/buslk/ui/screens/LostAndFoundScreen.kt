package com.buslk.ui.screens

// --- Mock Data ---
data class LostFoundItem(
    val id: String,
    val title: String,
    val description: String,
    val route: String,
    val location: String,
    val timeAgo: String,
    val reporterName: String,
    val isFound: Boolean,
    val isClosed: Boolean
)

val mockLostFoundItems = listOf(
    LostFoundItem(
        id = "1",
        title = "Black Backpack",
        description = "Small black backpack with laptop inside. Found near seat 12A.",
        route = "Route 138",
        location = "Seat 12A",
        timeAgo = "15 min ago",
        reporterName = "Amal P.",
        isFound = true,
        isClosed = false
    ),
    LostFoundItem(
        id = "2",
        title = "Phone Charger",
        description = "White iPhone charger cable with adapter",
        route = "Route 176",
        location = "Back seat area",
        timeAgo = "1 hour ago",
        reporterName = "Priya S.",
        isFound = false,
        isClosed = false
    ),
    LostFoundItem(
        id = "3",
        title = "Water Bottle",
        description = "Blue metal water bottle with university stickers",
        route = "Route 138",
        location = "Front rows",
        timeAgo = "3 hours ago",
        reporterName = "Kamal F.",
        isFound = true,
        isClosed = true
    ),
    LostFoundItem(
        id = "4",
        title = "Umbrella",
        description = "Black folding umbrella, brand new",
        route = "Route 120",
        location = "Luggage rack",
        timeAgo = "Yesterday",
        reporterName = "Saman D.",
        isFound = false,
        isClosed = false
    )
)