package com.retailpos.app.core.payment

import android.content.Context

/**
 * Persists transient checkout state so process recreation does not silently
 * forget a cash amount or generate a new idempotency key for a different bill.
 * Every value is tied to the exact recovered-cart fingerprint.
 */
object PendingPaymentStore {
    private const val PREFS = "retailpos_pending_payment"
    private const val KEY_AMOUNT_TENDERED = "amount_tendered"
    private const val KEY_IDEMPOTENCY = "checkout_idempotency"
    private const val KEY_CART_FINGERPRINT = "cart_fingerprint"

    @Volatile private var prefs: android.content.SharedPreferences? = null
    @Volatile private var amountTendered: Double? = null
    @Volatile private var idempotencyKey: String? = null
    @Volatile private var cartFingerprint: String? = null

    fun configure(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        amountTendered = prefs?.getString(KEY_AMOUNT_TENDERED, null)?.toDoubleOrNull()
        idempotencyKey = prefs?.getString(KEY_IDEMPOTENCY, null)?.takeIf { it.isNotBlank() }
        cartFingerprint = prefs?.getString(KEY_CART_FINGERPRINT, null)?.takeIf { it.isNotBlank() }
    }

    /** Backward-compatible setter; automatically binds the value to the current active cart. */
    fun set(amount: Double?) {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of)
        set(amount, fingerprint)
    }

    fun set(amount: Double?, fingerprint: String?) {
        amountTendered = amount
        cartFingerprint = fingerprint?.takeIf { it.isNotBlank() }
        prefs?.edit()?.apply {
            if (amount == null) remove(KEY_AMOUNT_TENDERED) else putString(KEY_AMOUNT_TENDERED, amount.toString())
            if (cartFingerprint == null) remove(KEY_CART_FINGERPRINT) else putString(KEY_CART_FINGERPRINT, cartFingerprint)
        }?.apply()
    }

    fun get(): Double? {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of) ?: return null
        return getAmountTendered(fingerprint)
    }

    fun getAmountTendered(fingerprint: String): Double? =
        if (cartFingerprint == fingerprint) amountTendered else null

    fun getOrCreateIdempotencyKey(fingerprint: String, create: () -> String): String {
        val existing = idempotencyKey
        if (!existing.isNullOrBlank() && cartFingerprint == fingerprint) return existing
        val created = create().takeIf { it.isNotBlank() } ?: error("Invalid checkout idempotency key")
        idempotencyKey = created
        cartFingerprint = fingerprint
        prefs?.edit()?.putString(KEY_IDEMPOTENCY, created)?.putString(KEY_CART_FINGERPRINT, fingerprint)?.apply()
        return created
    }

    /** Backward-compatible overload for older callers. */
    fun getOrCreateIdempotencyKey(create: () -> String): String {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of)
            ?: "NO_ACTIVE_CART"
        return getOrCreateIdempotencyKey(fingerprint, create)
    }

    fun clearIdempotencyKey() {
        idempotencyKey = null
        prefs?.edit()?.remove(KEY_IDEMPOTENCY)?.apply()
    }

    fun clear() {
        amountTendered = null
        idempotencyKey = null
        cartFingerprint = null
        prefs?.edit()?.remove(KEY_AMOUNT_TENDERED)?.remove(KEY_IDEMPOTENCY)?.remove(KEY_CART_FINGERPRINT)?.apply()
    }
}
