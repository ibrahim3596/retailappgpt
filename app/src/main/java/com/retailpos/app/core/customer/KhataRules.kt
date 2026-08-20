package com.retailpos.app.core.customer

object KhataRules {
    fun validatePayment(outstanding: Double, amount: Double): String? = when {
        !outstanding.isFinite() || outstanding < 0.0 -> "Outstanding balance is invalid"
        !amount.isFinite() || amount <= 0.0 -> "Payment must be greater than zero"
        outstanding <= 0.0 -> "There is no outstanding balance to collect"
        amount > outstanding + 0.000001 -> "Payment cannot exceed the outstanding amount"
        else -> null
    }

    fun balanceAfterPayment(outstanding: Double, amount: Double): Double? =
        validatePayment(outstanding, amount)?.let { null } ?: (outstanding - amount).coerceAtLeast(0.0)

    fun displayState(balance: Double): KhataState = when {
        !balance.isFinite() -> KhataState.INVALID
        balance > 0.000001 -> KhataState.DUE
        balance < -0.000001 -> KhataState.CREDIT
        else -> KhataState.SETTLED
    }
}

enum class KhataState { DUE, CREDIT, SETTLED, INVALID }
