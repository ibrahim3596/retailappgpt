package com.retailpos.app.core.payment

import android.content.Context

/**
 * Persists transient checkout tender data so a process recreation does not
 * silently forget a cash amount already entered by the cashier.
 */
object PendingPaymentStore {
    private const val PREFS = "retailpos_pending_payment"
    private const val KEY_AMOUNT_TENDERED = "amount_tendered"

    @Volatile private var prefs: android.content.SharedPreferences? = null
    @Volatile private var amountTendered: Double? = null

    fun configure(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        amountTendered = prefs?.getString(KEY_AMOUNT_TENDERED, null)?.toDoubleOrNull()
    }

    fun set(amount: Double?) {
        amountTendered = amount
        prefs?.edit()?.apply {
            if (amount == null) remove(KEY_AMOUNT_TENDERED)
            else putString(KEY_AMOUNT_TENDERED, amount.toString())
        }?.apply()
    }

    fun get(): Double? = amountTendered

    fun clear() {
        amountTendered = null
        prefs?.edit()?.remove(KEY_AMOUNT_TENDERED)?.apply()
    }
}
