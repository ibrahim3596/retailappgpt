package com.retailpos.app.core.staff

import com.retailpos.app.core.permissions.StaffPermission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffManagementRulesTest {
    @Test
    fun onlyOwnerCanManageStaff() {
        assertTrue(StaffSessionPermissions.can(com.retailpos.app.core.permissions.StaffRole.OWNER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffSessionPermissions.can(com.retailpos.app.core.permissions.StaffRole.MANAGER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffSessionPermissions.can(com.retailpos.app.core.permissions.StaffRole.CASHIER, StaffPermission.MANAGE_STAFF))
    }

    @Test
    fun discountLimitsRemainRoleSpecific() {
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(com.retailpos.app.core.permissions.StaffRole.OWNER) >= 100.0)
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(com.retailpos.app.core.permissions.StaffRole.MANAGER) <= 50.0)
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(com.retailpos.app.core.permissions.StaffRole.CASHIER) <= 10.0)
    }
}
