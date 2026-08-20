package com.retailpos.app.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun discountsCannotExceedSubtotal() {
        assertEquals(
            "Discount cannot exceed the bill subtotal.",
            StaffPermissionRules.validateBillDiscount(StaffRole.OWNER, 100.0, 100.01)
        )
    }

    @Test
    fun onlyOwnerAndManagerCanManageExpenses() {
        assertTrue(StaffPermissionRules.hasPermission(StaffRole.OWNER, StaffPermission.MANAGE_EXPENSES))
        assertTrue(StaffPermissionRules.hasPermission(StaffRole.MANAGER, StaffPermission.MANAGE_EXPENSES))
        assertFalse(StaffPermissionRules.hasPermission(StaffRole.CASHIER, StaffPermission.MANAGE_EXPENSES))
    }

    @Test
    fun cashierCannotProcessReturns() {
        assertFalse(StaffPermissionRules.hasPermission(StaffRole.CASHIER, StaffPermission.PROCESS_RETURN))
        assertTrue(StaffPermissionRules.hasPermission(StaffRole.MANAGER, StaffPermission.PROCESS_RETURN))
    }
}
