package com.example.data.model

data class DailyReadingStat(
    val dayLabel: String,     // "Mon", "Tue", etc.
    val dateLabel: String,    // "Oct 28"
    val minutesRead: Int,
    val pagesRead: Int,
    val isToday: Boolean = false
)

data class ReadingVelocityPoint(
    val label: String,        // "Mon", "Day 1", etc.
    val pagesRead: Int,
    val minutesRead: Int,
    val pagesPerHour: Float
)

data class ReadingStatsSummary(
    val totalReadingTimeSeconds: Long = 0L,
    val totalPagesRead: Int = 0,
    val totalSessionsCount: Int = 0,
    val averagePagesPerSession: Float = 0f,
    val averageMinutesPerSession: Float = 0f,
    val completedBooksCount: Int = 0,
    val inProgressBooksCount: Int = 0,
    val unreadBooksCount: Int = 0,
    val totalBooksCount: Int = 0,
    val currentStreakDays: Int = 0,
    val weeklyDayStats: List<DailyReadingStat> = emptyList(),
    val velocityPoints: List<ReadingVelocityPoint> = emptyList(),
    val formattedTotalTime: String = "0m"
)
