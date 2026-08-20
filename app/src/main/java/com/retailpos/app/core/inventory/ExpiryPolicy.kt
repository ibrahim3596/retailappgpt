package com.retailpos.app.core.inventory

object ExpiryPolicy {
    const val DEFAULT_WARNING_WINDOW_DAYS = 30L
    const val DAY_MS = 24L * 60L * 60L * 1000L

    fun warningWindowMs(days: Long = DEFAULT_WARNING_WINDOW_DAYS): Long = days.coerceAtLeast(0L) * DAY_MS

    fun shouldExcludeFromSale(expiryDate: Long?, now: Long): Boolean =
        expiryDate != null && expiryDate <= now
}
