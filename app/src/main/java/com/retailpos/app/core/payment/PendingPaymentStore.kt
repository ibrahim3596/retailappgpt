package com.retailpos.app.core.payment

import android.content.Context

/**
 * Persists transient checkout state so process recreation does not silently
 * forget a cash amount or reuse an idempotency key for different transaction inputs.
 */
object PendingPaymentStore {
    private const val PREFS = "retailpos_pending_payment"
    private const val KEY_AMOUNT_TENDERED = "amount_tendered"
    private const val KEY_AMOUNT_CART_FINGERPRINT = "amount_cart_fingerprint"
    private const val KEY_IDEMPOTENCY = "checkout_idempotency"
    private const val KEY_IDEMPOTENCY_FINGERPRINT = "idempotency_fingerprint"

    @Volatile private var prefs: android.content.SharedPreferences? = null
    @Volatile private var applicationContext: Context? = null
    @Volatile private var amountTendered: Double? = null
    @Volatile private var amountCartFingerprint: String? = null
    @Volatile private var idempotencyKey: String? = null
    @Volatile private var idempotencyFingerprint: String? = null

    fun configure(context: Context) {
        applicationContext = context.applicationContext
        prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        amountTendered = prefs?.getString(KEY_AMOUNT_TENDERED, null)?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
        amountCartFingerprint = prefs?.getString(KEY_AMOUNT_CART_FINGERPRINT, null)?.takeIf { it.isNotBlank() }
        idempotencyKey = prefs?.getString(KEY_IDEMPOTENCY, null)?.takeIf { it.isNotBlank() }
        idempotencyFingerprint = prefs?.getString(KEY_IDEMPOTENCY_FINGERPRINT, null)?.takeIf { it.isNotBlank() }
    }

    fun context(): Context = applicationContext ?: error("PendingPaymentStore has not been configured")

    /** Backward-compatible setter; binds cash state to the current cart only. */
    fun set(amount: Double?) {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of)
        set(amount, fingerprint)
    }

    fun set(amount: Double?, cartFingerprint: String?) {
        val sanitizedAmount = amount?.takeIf { it.isFinite() && it >= 0.0 }
        amountTendered = sanitizedAmount
        amountCartFingerprint = sanitizedAmount?.let { cartFingerprint?.takeIf { it.isNotBlank() } }
        prefs?.edit()?.apply {
            if (sanitizedAmount == null) remove(KEY_AMOUNT_TENDERED) else putString(KEY_AMOUNT_TENDERED, sanitizedAmount.toString())
            if (amountCartFingerprint == null) remove(KEY_AMOUNT_CART_FINGERPRINT) else putString(KEY_AMOUNT_CART_FINGERPRINT, amountCartFingerprint)
        }?.commit()
    }

    fun get(): Double? {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of) ?: return null
        return getAmountTenderedForCart(fingerprint)
    }

    fun getAmountTenderedForCart(cartFingerprint: String): Double? =
        if (amountCartFingerprint == cartFingerprint) amountTendered else null

    /** Serializes key creation so concurrent checkout attempts cannot mint two keys for one fingerprint. */
    @Synchronized
    fun getOrCreateIdempotencyKey(fingerprint: String, create: () -> String): String {
        val existing = idempotencyKey
        if (!existing.isNullOrBlank() && idempotencyFingerprint == fingerprint) return existing
        val created = create().takeIf { it.isNotBlank() } ?: error("Invalid checkout idempotency key")
        idempotencyKey = created
        idempotencyFingerprint = fingerprint
        prefs?.edit()?.putString(KEY_IDEMPOTENCY, created)?.putString(KEY_IDEMPOTENCY_FINGERPRINT, fingerprint)?.commit()
        return created
    }

    /** Backward-compatible overload for older callers. */
    fun getOrCreateIdempotencyKey(create: () -> String): String {
        val fingerprint = ActiveCartStore.load().takeIf { it.isNotEmpty() }?.let(CheckoutRecoveryFingerprint::of)
            ?: "NO_ACTIVE_CART"
        return getOrCreateIdempotencyKey(fingerprint, create)
    }

    @Synchronized
    fun clearIdempotencyKey() {
        idempotencyKey = null
        idempotencyFingerprint = null
        prefs?.edit()?.remove(KEY_IDEMPOTENCY)?.remove(KEY_IDEMPOTENCY_FINGERPRINT)?.commit()
    }

    @Synchronized
    fun clear() {
        amountTendered = null
        amountCartFingerprint = null
        idempotencyKey = null
        idempotencyFingerprint = null
        prefs?.edit()
            ?.remove(KEY_AMOUNT_TENDERED)
            ?.remove(KEY_AMOUNT_CART_FINGERPRINT)
            ?.remove(KEY_IDEMPOTENCY)
            ?.remove(KEY_IDEMPOTENCY_FINGERPRINT)
            ?.commit()
    }
}
