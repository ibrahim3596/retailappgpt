package com.retailpos.app.core.products

/**
 * Store/product pricing rules used by checkout. Tax is deliberately opt-in.
 * Product tax rates are supplied by the persisted product configuration once tax settings exist.
 */
enum class TaxTreatment {
    NO_TAX,
    GST_ADDED,
    GST_INCLUSIVE
}

data class PricingInput(
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxRatePercent: Double = 0.0,
    val taxTreatment: TaxTreatment = TaxTreatment.NO_TAX
)

data class PricingResult(
    val subtotal: Double,
    val discountAmount: Double,
    val taxableAmount: Double,
    val taxAmount: Double,
    val total: Double
)

object PricingRules {
    fun calculate(input: PricingInput): PricingResult {
        require(input.subtotal.isFinite() && input.subtotal >= 0.0) { "Subtotal must be non-negative and finite." }
        require(input.discountAmount.isFinite() && input.discountAmount >= 0.0) { "Discount must be non-negative and finite." }
        require(input.discountAmount <= input.subtotal + 1e-9) { "Discount cannot exceed subtotal." }
        require(input.taxRatePercent.isFinite() && input.taxRatePercent >= 0.0 && input.taxRatePercent <= 100.0) {
            "Tax rate must be between 0 and 100 percent."
        }

        val taxable = roundCurrency((input.subtotal - input.discountAmount).coerceAtLeast(0.0))
        val tax = when (input.taxTreatment) {
            TaxTreatment.NO_TAX -> 0.0
            TaxTreatment.GST_ADDED -> roundCurrency(taxable * input.taxRatePercent / 100.0)
            TaxTreatment.GST_INCLUSIVE ->
                if (input.taxRatePercent == 0.0) 0.0
                else roundCurrency(taxable - (taxable / (1.0 + input.taxRatePercent / 100.0)))
        }
        val total = when (input.taxTreatment) {
            TaxTreatment.GST_ADDED -> roundCurrency(taxable + tax)
            TaxTreatment.NO_TAX -> taxable
            TaxTreatment.GST_INCLUSIVE -> taxable
        }

        return PricingResult(
            subtotal = roundCurrency(input.subtotal),
            discountAmount = roundCurrency(input.discountAmount),
            taxableAmount = taxable,
            taxAmount = tax,
            total = total
        )
    }

    /** Currency rounding must use decimal half-up semantics, not binary floating-point rounding. */
    private fun roundCurrency(value: Double): Double =
        java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
}
