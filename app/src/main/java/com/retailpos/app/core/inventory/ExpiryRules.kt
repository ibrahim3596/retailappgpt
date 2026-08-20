package com.retailpos.app.core.inventory

import java.util.concurrent.TimeUnit

enum class ExpiryStatus { FRESH, NEAR_EXPIRY, EXPIRED }

object ExpiryRules {
    fun status(expiryDate: Long?, now: Long = System.currentTimeMillis(), nearExpiryDays: Long = 30): ExpiryStatus {
        if (expiryDate == null) return ExpiryStatus.FRESH
        if (expiryDate <= now) return ExpiryStatus.EXPIRED
        val remaining = expiryDate - now
        return if (remaining <= TimeUnit.DAYS.toMillis(nearExpiryDays)) ExpiryStatus.NEAR_EXPIRY else ExpiryStatus.FRESH
    }
}
