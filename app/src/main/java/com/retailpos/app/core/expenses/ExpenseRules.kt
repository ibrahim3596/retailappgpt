package com.retailpos.app.core.expenses

object ExpenseRules {
    val categories = listOf("RENT", "ELECTRICITY", "STAFF", "TRANSPORT", "PACKAGING", "REPAIRS", "MISC")
    val paymentMethods = listOf("CASH", "UPI", "CARD", "OTHER")

    fun validate(amount: Double, category: String, paymentMethod: String, note: String): String? {
        if (!amount.isFinite() || amount <= 0.0) return "Expense amount must be greater than zero."
        if (category !in categories) return "Select a valid expense category."
        if (paymentMethod !in paymentMethods) return "Select a valid payment method."
        if (note.length > 500) return "Expense note is too long."
        return null
    }
}
