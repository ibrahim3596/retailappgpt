package com.retailpos.app.core.pos

import com.retailpos.app.core.permissions.StaffPermission
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.staff.StaffRole
import com.retailpos.app.data.CartLine
import kotlin.math.abs

object CartLinePricingRules {
    private const val EPSILON = 1e-9

    fun validate(line: CartLine, role: StaffRole): String? {
        if (line.quantity <= 0.0 || !line.quantity.isFinite()) return "Quantity must be greater than zero."
        if (line.unitPrice < 0.0 || !line.unitPrice.isFinite()) return "Original selling price is invalid."

        val override = line.overrideUnitPrice
        if (override != null) {
            if (!override.isFinite() || override < 0.0) return "Override price is invalid."
            StaffPermissionRules.validatePriceOverride(role)?.let { return it }
        }

        val discount = line.itemDiscountAmount
        if (!discount.isFinite() || discount < 0.0) return "Item discount is invalid."
        if (!StaffPermissionRules.hasPermission(role, StaffPermission.APPLY_ITEM_DISCOUNT) && discount > EPSILON) {
            return "This staff role cannot apply item discounts."
        }
        val gross = (override ?: line.unitPrice) * line.quantity
        if (discount > gross + EPSILON) return "Item discount cannot exceed the line value."
        if (!gross.isFinite()) return "Line value is invalid."
        return null
    }

    fun apply(line: CartLine, overrideUnitPrice: Double?, itemDiscountAmount: Double, role: StaffRole): CartLine {
        val updated = line.copy(
            overrideUnitPrice = overrideUnitPrice,
            itemDiscountAmount = itemDiscountAmount
        )
        require(validate(updated, role) == null) { validate(updated, role)!! }
        return updated
    }

    fun hasOverrides(line: CartLine): Boolean =
        line.overrideUnitPrice != null || abs(line.itemDiscountAmount) > EPSILON
}
