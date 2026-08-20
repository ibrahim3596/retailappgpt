package com.retailpos.app.core.purchasing

object SupplierPayableRules {
    fun balance(invoiceTotal: Double, paidAmount: Double): Double {
        require(invoiceTotal.isFinite() && invoiceTotal >= 0.0) { "Invoice total is invalid" }
        require(paidAmount.isFinite() && paidAmount >= 0.0) { "Paid amount is invalid" }
        require(paidAmount <= invoiceTotal + 1e-9) { "Paid amount cannot exceed invoice total" }
        return invoiceTotal - paidAmount
    }

    fun state(balance: Double): SupplierPayableState = when {
        !balance.isFinite() || balance < -1e-9 -> SupplierPayableState.INVALID
        balance <= 1e-9 -> SupplierPayableState.SETTLED
        else -> SupplierPayableState.OUTSTANDING
    }
}

enum class SupplierPayableState { OUTSTANDING, SETTLED, INVALID }
