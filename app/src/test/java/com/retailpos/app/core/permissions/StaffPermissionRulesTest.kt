package com.retailpos.app.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StaffPermissionRulesTest {
    @Test
    fun cashierCanApplyUpToTenPercentBillDiscount() {
        assertNull(StaffPermissionRules.validateBillDiscount(StaffRole.CASHIER, 1000.0, 100.0))
        assertEquals(
            "Cashier discount limit is 10.0%.",
            StaffPermissionRules.validateBillDiscount(StaffRole.CASHIER, 1000.0, 100.01)
        )
    }

    @Test
    fun managerCanApplyUpToFiftyPercent() {
        assertNull(StaffPermissionRules.validateBillDiscount(StaffRole.MANAGER, 1000.0, 500.0))
        assertEquals(
            "Manager discount limit is 50.0%.",
            StaffPermissionRules.validateBillDiscount(StaffRole.MANAGER, 1000.0, 500.01)
        )
    }

    @Test
    fun cashierCannotOverrideSellingPrice() {
        assertEquals(
            "This staff role cannot override selling prices.",
            StaffPermissionRules.validatePriceOverride(StaffRole.CASHIER)
        )
        assertNull(StaffPermissionRules.validatePriceOverride(StaffRole.MANAGER))
    }

    @Test
    fun cashierCannotApplyItemDiscount() {
        assertEquals(
            "Cashiers cannot apply item-level discounts.",
            StaffPermissionRules.validateItemDiscount(StaffRole.CASHIER, 100.0, 1.0)
        )
    }

    @Test
    fun managerItemDiscountIsCappedAtThirtyPercent() {
        assertNull(StaffPermissionRules.validateItemDiscount(StaffRole.MANAGER, 100.0, 30.0))
        assertEquals(
            "Manager item discount limit is 30.0%.",
            StaffPermissionRules.validateItemDiscount(StaffRole.MANAGER, 100.0, 30.01)
        )
    }

    @Test
    fun discountsCannotExceedSubtotal() {
        assertEquals(
            "Discount cannot exceed the bill subtotal.",
            StaffPermissionRules.validateBillDiscount(StaffRole.OWNER, 100.0, 100.01)
        )
    }
}
