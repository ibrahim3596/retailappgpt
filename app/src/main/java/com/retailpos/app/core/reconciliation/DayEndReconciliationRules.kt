package com.retailpos.app.core.reconciliation

import com.retailpos.app.data.SaleEntity

object DayEndReconciliationRules {
    fun summarize(sales: List<SaleEntity>): DayEndSummary {
        val cash = sales.filter { it.paymentMethod.equals("CASH", true) }.sumOf { it.total }
        val upi = sales.filter { it.paymentMethod.equals("UPI", true) }.sumOf { it.total }
        val card = sales.filter { it.paymentMethod.equals("CARD", true) }.sumOf { it.total }
        val credit = sales.filter { it.paymentMethod.equals("CREDIT", true) }.sumOf { it.total }
        val other = sales.filter { method -> method.paymentMethod.uppercase() !in setOf("CASH", "UPI", "CARD", "CREDIT") }.sumOf { it.total }
        return DayEndSummary(
            billCount = sales.size,
            totalSales = sales.sumOf { it.total },
            cashSales = cash,
            upiSales = upi,
            cardSales = card,
            creditSales = credit,
            otherSales = other
        )
    }

    fun cashDifference(expectedCash: Double, countedCash: Double): Double {
        require(expectedCash.isFinite() && expectedCash >= 0.0) { "Expected cash is invalid" }
        require(countedCash.isFinite() && countedCash >= 0.0) { "Counted cash is invalid" }
        return countedCash - expectedCash
    }
}

data class DayEndSummary(
    val billCount: Int,
    val totalSales: Double,
    val cashSales: Double,
    val upiSales: Double,
    val cardSales: Double,
    val creditSales: Double,
    val otherSales: Double
)
