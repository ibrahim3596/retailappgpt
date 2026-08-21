package com.retailpos.app.core.purchasing

object PurchaseRules {
    fun lineEconomics(line: PurchaseLineDraft): PurchaseLineEconomics {
        require(line.orderedQuantity.isFinite() && line.orderedQuantity > 0.0) { "Ordered quantity must be greater than zero" }
        require(line.freeQuantity.isFinite() && line.freeQuantity >= 0.0) { "Free quantity cannot be negative" }
        require(line.purchaseRate.isFinite() && line.purchaseRate >= 0.0) { "Purchase rate cannot be negative" }
        require(line.schemeDiscount.isFinite() && line.schemeDiscount >= 0.0) { "Scheme discount cannot be negative" }
        require(line.schemeDiscount <= line.orderedQuantity * line.purchaseRate + 1e-9) { "Scheme discount cannot exceed gross cost" }
        require(line.expiryDate == null || line.batchNumber != null) { "Expiry requires a batch number" }

        val gross = line.orderedQuantity * line.purchaseRate
        val net = gross - line.schemeDiscount
        val stock = line.orderedQuantity + line.freeQuantity
        val effective = if (stock > 0.0) net / stock else 0.0
        return PurchaseLineEconomics(line.orderedQuantity, line.freeQuantity, stock, gross, line.schemeDiscount, net, effective)
    }

    fun validateDraft(draft: PurchaseDraft): List<String> {
        val errors = mutableListOf<String>()
        if (draft.supplierId.isBlank()) errors += "Supplier is required"
        if (draft.lines.isEmpty()) errors += "At least one purchase line is required"
        if (!draft.paidAmount.isFinite() || draft.paidAmount < 0.0) errors += "Paid amount must be non-negative"

        val validEconomics = draft.lines.mapIndexedNotNull { index, line ->
            try {
                lineEconomics(line)
            } catch (e: IllegalArgumentException) {
                errors += "Line ${index + 1}: ${e.message}"
                null
            }
        }
        val total = validEconomics.sumOf { it.netCost }
        if (draft.paidAmount > total + 1e-9) errors += "Paid amount cannot exceed purchase total"
        return errors
    }

    fun payableAmount(total: Double, paid: Double): Double {
        require(total.isFinite() && total >= 0.0) { "Purchase total is invalid" }
        require(paid.isFinite() && paid >= 0.0) { "Paid amount is invalid" }
        require(paid <= total + 1e-9) { "Paid amount cannot exceed purchase total" }
        return total - paid
    }
}
