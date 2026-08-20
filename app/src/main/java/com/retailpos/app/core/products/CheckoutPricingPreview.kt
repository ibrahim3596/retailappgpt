package com.retailpos.app.core.products

import com.retailpos.app.data.CartLine

data class CheckoutLinePreview(
    val productId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val taxableAmount: Double,
    val discountAmount: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val total: Double
)

data class CheckoutPricingPreview(
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val lines: List<CheckoutLinePreview>
)

object CheckoutPricingPreviewCalculator {
    fun calculate(
        cart: List<CartLine>,
        taxTreatment: TaxTreatment,
        taxRatesByProductId: Map<String, Double>,
        billDiscountAmount: Double = 0.0
    ): CheckoutPricingPreview {
        val subtotal = cart.sumOf { it.lineTotal }
        val safeDiscount = PricingRules.calculate(
            PricingInput(
                subtotal = subtotal,
                discountAmount = billDiscountAmount,
                taxTreatment = TaxTreatment.NO_TAX
            )
        ).discountAmount

        val lines = cart.map { line ->
            val lineDiscount = if (subtotal <= 0.0) 0.0 else safeDiscount * (line.lineTotal / subtotal)
            val rate = if (taxTreatment == TaxTreatment.NO_TAX) 0.0 else (taxRatesByProductId[line.productId] ?: 0.0)
            val pricing = PricingRules.calculate(
                PricingInput(
                    subtotal = line.lineTotal,
                    discountAmount = lineDiscount,
                    taxRatePercent = rate,
                    taxTreatment = taxTreatment
                )
            )
            CheckoutLinePreview(
                productId = line.productId,
                name = line.name,
                quantity = line.quantity,
                unit = line.unit,
                unitPrice = line.unitPrice,
                taxableAmount = pricing.taxableAmount,
                discountAmount = pricing.discountAmount,
                taxRatePercent = rate,
                taxAmount = pricing.taxAmount,
                total = pricing.total
            )
        }

        return CheckoutPricingPreview(
            subtotal = subtotal,
            discountAmount = lines.sumOf { it.discountAmount },
            taxAmount = lines.sumOf { it.taxAmount },
            total = lines.sumOf { it.total },
            lines = lines
        )
    }
}
