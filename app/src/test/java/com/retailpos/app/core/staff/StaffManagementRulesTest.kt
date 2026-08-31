package com.retailpos.app.core.staff

import com.retailpos.app.core.permissions.StaffPermission
import com.retailpos.app.core.permissions.StaffRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffManagementRulesTest {
    @Test
    fun onlyOwnerCanManageStaff() {
        assertTrue(StaffSessionPermissions.can(StaffRole.OWNER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffSessionPermissions.can(StaffRole.MANAGER, StaffPermission.MANAGE_STAFF))
        assertFalse(StaffSessionPermissions.can(StaffRole.CASHIER, StaffPermission.MANAGE_STAFF))
    }

    @Test
    fun discountLimitsRemainRoleSpecific() {
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(StaffRole.OWNER) >= 100.0)
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(StaffRole.MANAGER) <= 50.0)
        assertTrue(StaffSessionPermissions.maxBillDiscountPercent(StaffRole.CASHIER) <= 10.0)
    }

    @Test
    fun lastActiveOwnerCannotBeDisabled() {
        assertFalse(StaffManagementRules.canDeactivate(StaffRole.OWNER, true, false, 1))
        assertTrue(StaffManagementRules.canDeactivate(StaffRole.OWNER, true, false, 2))
    }

    @Test
    fun lastActiveOwnerCannotBeDemoted() {
        assertFalse(StaffManagementRules.canChangeRole(StaffRole.OWNER, true, StaffRole.MANAGER, 1))
        assertTrue(StaffManagementRules.canChangeRole(StaffRole.OWNER, true, StaffRole.MANAGER, 2))
    }

    @Test
    fun promotingStaffToOwnerRemainsAllowed() {
        assertTrue(StaffManagementRules.canChangeRole(StaffRole.MANAGER, true, StaffRole.OWNER, 1))
    }

    @Test
    fun nonOwnerCanBeDisabledWhenNoOwnersExist() {
        assertTrue(StaffManagementRules.canDeactivate(StaffRole.CASHIER, true, false, 0))
    }
}
