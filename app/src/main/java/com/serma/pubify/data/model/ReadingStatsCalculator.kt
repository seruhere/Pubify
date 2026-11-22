package com.serma.pubify.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReadingStatsCalculator {

    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "0m"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${maxOf(1L, minutes)}m"
        }
    }

    fun calculate(
        books: List<ConvertedBook>,
        sessions: List<ReadingSession>,
        today: LocalDate = LocalDate.now()
    ): ReadingStatsSummary {
        val totalTimeSeconds = sessions.sumOf { it.durationSeconds }
        val totalPagesRead = sessions.sumOf { it.pagesRead }
        val totalSessions = sessions.size

        val avgPages = if (totalSessions > 0) {
            totalPagesRead.toFloat() / totalSessions.toFloat()
        } else 0f

        val avgMinutes = if (totalSessions > 0) {
            (totalTimeSeconds.toFloat() / 60f) / totalSessions.toFloat()
        } else 0f

        // Books stats
        var completedCount = 0
        var inProgressCount = 0
        var unreadCount = 0

        for (book in books) {
            val isCompleted = book.isCompleted || (book.chapterCount > 1 && book.lastReadChapterIndex >= book.chapterCount - 1)
            val isInProgress = !isCompleted && (book.lastReadChapterIndex > 0 || book.lastReadScrollOffset > 0)
            when {
                isCompleted -> completedCount++
                isInProgress -> inProgressCount++
                else -> unreadCount++
            }
        }

        // Streak calculation
        val sessionDates = sessions.mapNotNull { session ->
            try {
                Instant.ofEpochMilli(session.timestampMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: Exception) {
                null
            }
        }.toSet()

        var streak = 0
        var checkDate = today
        // Check if user read today; if not, check if they read yesterday to continue streak
        if (!sessionDates.contains(checkDate)) {
            checkDate = today.minusDays(1)
        }
        while (sessionDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        // Weekly activity: Last 7 days ending today
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val weeklyStats = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val daysSessions = sessions.filter { session ->
                try {
                    val sDate = Instant.ofEpochMilli(session.timestampMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    sDate == date
                } catch (_: Exception) {
                    false
                }
            }
            val minRead = (daysSessions.sumOf { it.durationSeconds } / 60).toInt()
            val pRead = daysSessions.sumOf { it.pagesRead }
            DailyReadingStat(
                dayLabel = date.format(dayFormatter),
                dateLabel = date.format(dateFormatter),
                minutesRead = minRead,
                pagesRead = pRead,
                isToday = (date == today)
            )
        }

        // Velocity trend across the 7 days
        val velocityPoints = weeklyStats.map { dayStat ->
            val hours = dayStat.minutesRead.toFloat() / 60f
            val pagesPerHour = if (hours > 0.05f) {
                dayStat.pagesRead.toFloat() / hours
            } else if (dayStat.pagesRead > 0) {
                dayStat.pagesRead.toFloat() * 1.5f
            } else 0f
            ReadingVelocityPoint(
                label = dayStat.dayLabel,
                pagesRead = dayStat.pagesRead,
                minutesRead = dayStat.minutesRead,
                pagesPerHour = pagesPerHour
            )
        }

        return ReadingStatsSummary(
            totalReadingTimeSeconds = totalTimeSeconds,
            totalPagesRead = totalPagesRead,
            totalSessionsCount = totalSessions,
            averagePagesPerSession = avgPages,
            averageMinutesPerSession = avgMinutes,
            completedBooksCount = completedCount,
            inProgressBooksCount = inProgressCount,
            unreadBooksCount = unreadCount,
            totalBooksCount = books.size,
            currentStreakDays = streak,
            weeklyDayStats = weeklyStats,
            velocityPoints = velocityPoints,
            formattedTotalTime = formatDuration(totalTimeSeconds)
        )
    }
}
