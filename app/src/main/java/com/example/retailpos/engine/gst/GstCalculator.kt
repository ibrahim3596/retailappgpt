package com.example.retailpos.engine.gst

import kotlin.math.roundToInt

data class GstBreakdown(
    val grossPrice: Double,
    val discountAmount: Double,
    val assessableValue: Double,
    val gstRate: Double,
    val isTaxInclusive: Boolean,
    val isInterstate: Boolean,
    val totalGst: Double,
    val cgstRate: Double,
    val cgstAmount: Double,
    val sgstRate: Double,
    val sgstAmount: Double,
    val igstRate: Double,
    val igstAmount: Double,
    val finalLineTotal: Double
)

object GstCalculator {

    private fun round2(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    fun calculateItemGst(
        sellingPrice: Double,
        quantity: Double,
        gstRate: Double,
        isTaxInclusive: Boolean,
        discountAmount: Double = 0.0,
        isInterstate: Boolean = false
    ): GstBreakdown {
        val grossPrice = round2(sellingPrice * quantity)
        val discountedGross = (grossPrice - discountAmount).coerceAtLeast(0.0)

        val assessableValue: Double
        val totalGst: Double

        if (gstRate <= 0.0) {
            assessableValue = discountedGross
            totalGst = 0.0
        } else if (isTaxInclusive) {
            // Price = AssessableValue * (1 + rate/100)
            assessableValue = round2(discountedGross / (1.0 + (gstRate / 100.0)))
            totalGst = round2(discountedGross - assessableValue)
        } else {
            // Price is Exclusive
            assessableValue = discountedGross
            totalGst = round2(discountedGross * (gstRate / 100.0))
        }

        val cgstRate: Double
        val cgstAmount: Double
        val sgstRate: Double
        val sgstAmount: Double
        val igstRate: Double
        val igstAmount: Double

        if (isInterstate) {
            cgstRate = 0.0
            cgstAmount = 0.0
            sgstRate = 0.0
            sgstAmount = 0.0
            igstRate = gstRate
            igstAmount = totalGst
        } else {
            cgstRate = gstRate / 2.0
            cgstAmount = round2(totalGst / 2.0)
            sgstRate = gstRate / 2.0
            sgstAmount = round2(totalGst - cgstAmount) // ensure exact sum
            igstRate = 0.0
            igstAmount = 0.0
        }

        val finalLineTotal = if (isTaxInclusive) {
            discountedGross
        } else {
            round2(discountedGross + totalGst)
        }

        return GstBreakdown(
            grossPrice = grossPrice,
            discountAmount = discountAmount,
            assessableValue = assessableValue,
            gstRate = gstRate,
            isTaxInclusive = isTaxInclusive,
            isInterstate = isInterstate,
            totalGst = totalGst,
            cgstRate = cgstRate,
            cgstAmount = cgstAmount,
            sgstRate = sgstRate,
            sgstAmount = sgstAmount,
            igstRate = igstRate,
            igstAmount = igstAmount,
            finalLineTotal = finalLineTotal
        )
    }
}
