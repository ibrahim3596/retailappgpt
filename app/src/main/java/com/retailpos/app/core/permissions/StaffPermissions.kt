package com.retailpos.app.core.permissions

import com.retailpos.app.core.staff.StaffRole

enum class StaffPermission {
    APPLY_BILL_DISCOUNT,
    APPLY_ITEM_DISCOUNT,
    OVERRIDE_SELLING_PRICE,
    VOID_BILL,
    PROCESS_RETURN,
    VIEW_REPORTS,
    MANAGE_PRODUCTS,
    MANAGE_STAFF,
    CHANGE_STORE_SETTINGS
}

data class DiscountAuthorization(
    val allowed: Boolean,
    val maxPercent: Double,
    val reason: String? = null
)

object StaffPermissionRules {
    fun hasPermission(role: StaffRole, permission: StaffPermission): Boolean = when (role) {
        StaffRole.OWNER -> true
        StaffRole.MANAGER -> permission != StaffPermission.MANAGE_STAFF
        StaffRole.CASHIER -> permission in setOf(
            StaffPermission.APPLY_BILL_DISCOUNT
        )
    }

    fun billDiscountAuthorization(role: StaffRole): DiscountAuthorization = when (role) {
        StaffRole.OWNER -> DiscountAuthorization(true, 100.0)
        StaffRole.MANAGER -> DiscountAuthorization(true, 50.0)
        StaffRole.CASHIER -> DiscountAuthorization(true, 10.0)
    }

    fun validateBillDiscount(role: StaffRole, subtotal: Double, amount: Double): String? {
        if (!subtotal.isFinite() || subtotal < 0.0) return "Invalid bill subtotal."
        if (!amount.isFinite() || amount < 0.0) return "Discount must be non-negative and finite."
        if (amount > subtotal + 1e-9) return "Discount cannot exceed the bill subtotal."
        val authorization = billDiscountAuthorization(role)
        if (!authorization.allowed) return "This staff role cannot apply bill discounts."
        val percent = if (subtotal == 0.0) 0.0 else amount * 100.0 / subtotal
        if (percent > authorization.maxPercent + 1e-9) {
            return "${role.name.lowercase().replaceFirstChar { it.uppercase() }} discount limit is ${authorization.maxPercent}%."
        }
        return null
    }

    fun validatePriceOverride(role: StaffRole): String? =
        if (hasPermission(role, StaffPermission.OVERRIDE_SELLING_PRICE)) null
        else "This staff role cannot override selling prices."
}
