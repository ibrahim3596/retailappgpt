package com.retailpos.app.core.staff

import com.retailpos.app.core.permissions.StaffPermission
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
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
        assertTrue(StaffPermissionRules.hasPermission(StaffRole.CASHIER, StaffPermission.APPLY_BILL_DISCOUNT))
        assertFalse(StaffPermissionRules.hasPermission(StaffRole.CASHIER, StaffPermission.OVERRIDE_SELLING_PRICE))
        assertTrue(StaffPermissionRules.billDiscountAuthorization(StaffRole.CASHIER).maxPercent <= 10.0)
    }

    @Test
    fun ownerCanManageStaff() {
        assertTrue(StaffPermissionRules.hasPermission(StaffRole.OWNER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffPermissionRules.hasPermission(StaffRole.MANAGER, StaffPermission.MANAGE_STAFF))
    }
}
