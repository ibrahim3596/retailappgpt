package com.retailpos.app.core.reconciliation

object DayEndRules {
    fun cashDifference(expected: Double, counted: Double): Double? {
        if (!expected.isFinite() || expected < 0.0 || !counted.isFinite() || counted < 0.0) return null
        return counted - expected
    }

    fun isBalanced(expected: Double, counted: Double, tolerance: Double = 0.01): Boolean =
        cashDifference(expected, counted)?.let { kotlin.math.abs(it) <= tolerance } == true
}
