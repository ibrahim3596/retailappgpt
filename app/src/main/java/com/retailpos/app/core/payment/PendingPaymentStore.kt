package com.retailpos.app.core.payment

object PendingPaymentStore {
    @Volatile private var amountTendered: Double? = null

    fun set(amount: Double?) {
        amountTendered = amount
    }

    fun get(): Double? = amountTendered

    fun clear() {
        amountTendered = null
    }
}
