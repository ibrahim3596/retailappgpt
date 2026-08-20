package com.retailpos.app.core.payment

import android.content.Context

/**
 * Persists transient checkout state so process recreation does not silently
 * forget a cash amount or generate a new idempotency key for the same pending bill.
 */
object PendingPaymentStore {
    private const val PREFS = "retailpos_pending_payment"
    private const val KEY_AMOUNT_TENDERED = "amount_tendered"
    private const val KEY_IDEMPOTENCY = "checkout_idempotency"

    @Volatile private var prefs: android.content.SharedPreferences? = null
    @Volatile private var amountTendered: Double? = null
    @Volatile private var idempotencyKey: String? = null

    fun configure(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        amountTendered = prefs?.getString(KEY_AMOUNT_TENDERED, null)?.toDoubleOrNull()
        idempotencyKey = prefs?.getString(KEY_IDEMPOTENCY, null)?.takeIf { it.isNotBlank() }
    }

    fun set(amount: Double?) {
        amountTendered = amount
        prefs?.edit()?.apply {
            if (amount == null) remove(KEY_AMOUNT_TENDERED)
            else putString(KEY_AMOUNT_TENDERED, amount.toString())
        }?.apply()
    }

    fun get(): Double? = amountTendered

    fun getOrCreateIdempotencyKey(create: () -> String): String {
        val existing = idempotencyKey
        if (!existing.isNullOrBlank()) return existing
        val created = create().takeIf { it.isNotBlank() } ?: error("Invalid checkout idempotency key")
        idempotencyKey = created
        prefs?.edit()?.putString(KEY_IDEMPOTENCY, created)?.apply()
        return created
    }

    fun clearIdempotencyKey() {
        idempotencyKey = null
        prefs?.edit()?.remove(KEY_IDEMPOTENCY)?.apply()
    }

    fun clear() {
        amountTendered = null
        idempotencyKey = null
        prefs?.edit()?.remove(KEY_AMOUNT_TENDERED)?.remove(KEY_IDEMPOTENCY)?.apply()
    }
}
