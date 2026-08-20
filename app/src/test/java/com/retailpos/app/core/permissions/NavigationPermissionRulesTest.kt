package com.retailpos.app.core.permissions

import com.retailpos.app.core.staff.StaffRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPermissionRulesTest {
    @Test
    fun cashierIsLimitedToOperationalScreens() {
        assertFalse(NavigationPermissionRules.canOpenProducts(StaffRole.CASHIER))
        assertFalse(NavigationPermissionRules.canOpenInventory(StaffRole.CASHIER))
        assertFalse(NavigationPermissionRules.canOpenAnalytics(StaffRole.CASHIER))
        assertFalse(NavigationPermissionRules.canOpenSettings(StaffRole.CASHIER))
    }

    @Test
    fun managerCanOpenOperationalManagementScreens() {
        assertTrue(NavigationPermissionRules.canOpenProducts(StaffRole.MANAGER))
        assertTrue(NavigationPermissionRules.canOpenInventory(StaffRole.MANAGER))
        assertTrue(NavigationPermissionRules.canOpenAnalytics(StaffRole.MANAGER))
        assertTrue(NavigationPermissionRules.canOpenSettings(StaffRole.MANAGER))
        assertFalse(NavigationPermissionRules.canOpenStaffManagement(StaffRole.MANAGER))
    }

    @Test
    fun ownerCanOpenEverything() {
        assertTrue(NavigationPermissionRules.canOpenProducts(StaffRole.OWNER))
        assertTrue(NavigationPermissionRules.canOpenInventory(StaffRole.OWNER))
        assertTrue(NavigationPermissionRules.canOpenAnalytics(StaffRole.OWNER))
        assertTrue(NavigationPermissionRules.canOpenSettings(StaffRole.OWNER))
        assertTrue(NavigationPermissionRules.canOpenStaffManagement(StaffRole.OWNER))
    }
}
