package com.aj.wafatti.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeFormatter {

    fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(date)
    }

    fun formatDateHeader(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return format.format(date)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val date = Date(timestamp)

        return when {
            diff < 24 * 60 * 60 * 1000 -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            }
            else -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }
}
