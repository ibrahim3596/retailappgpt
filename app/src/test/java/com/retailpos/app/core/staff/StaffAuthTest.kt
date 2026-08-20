package com.retailpos.app.core.staff

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffAuthTest {
    @Test
    fun validPinHashesAndVerifies() {
        val hash = StaffPinHasher.hash("1234", ByteArray(16) { 7 })
        assertTrue(StaffPinHasher.verify("1234", hash))
        assertFalse(StaffPinHasher.verify("4321", hash))
    }

    @Test
    fun invalidPinsAreRejected() {
        assertFalse(StaffPinPolicy.validatePin("123"))
        assertFalse(StaffPinPolicy.validatePin("123456789"))
        assertFalse(StaffPinPolicy.validatePin("12a4"))
        assertTrue(StaffPinPolicy.validatePin("1234"))
    }

    @Test
    fun cashierHasControlledDiscountLimit() {
        assertTrue(StaffPermissionRules.can(StaffRole.CASHIER, StaffPermission.BILL_DISCOUNT))
        assertFalse(StaffPermissionRules.can(StaffRole.CASHIER, StaffPermission.PRICE_OVERRIDE))
        assertTrue(StaffPermissionRules.maxBillDiscountPercent(StaffRole.CASHIER) <= 10.0)
    }

    @Test
    fun ownerCanManageStaff() {
        assertTrue(StaffPermissionRules.can(StaffRole.OWNER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffPermissionRules.can(StaffRole.MANAGER, StaffPermission.MANAGE_STAFF))
    }
}
