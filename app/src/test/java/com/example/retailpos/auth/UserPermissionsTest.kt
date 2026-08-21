package com.example.retailpos.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPermissionsTest {
    @Test
    fun `owner has administrative permissions`() {
        assertTrue(UserPermissions.canManageStaff(UserRole.OWNER))
        assertTrue(UserPermissions.canUpdateStoreProfile(UserRole.OWNER))
        assertTrue(UserPermissions.canAccessInventory(UserRole.OWNER))
        assertTrue(UserPermissions.canAccessAnalytics(UserRole.OWNER))
    }

    @Test
    fun `manager can operate inventory but cannot administer store`() {
        assertTrue(UserPermissions.canAccessInventory(UserRole.MANAGER))
        assertTrue(UserPermissions.canAdjustStock(UserRole.MANAGER))
        assertTrue(UserPermissions.canAccessAnalytics(UserRole.MANAGER))
        assertFalse(UserPermissions.canManageStaff(UserRole.MANAGER))
        assertFalse(UserPermissions.canUpdateStoreProfile(UserRole.MANAGER))
    }

    @Test
    fun `cashier cannot access management modules`() {
        assertTrue(UserPermissions.canPerformBilling(UserRole.CASHIER))
        assertTrue(UserPermissions.canManageCustomers(UserRole.CASHIER))
        assertFalse(UserPermissions.canAccessInventory(UserRole.CASHIER))
        assertFalse(UserPermissions.canAdjustStock(UserRole.CASHIER))
        assertFalse(UserPermissions.canAccessAnalytics(UserRole.CASHIER))
        assertFalse(UserPermissions.canManageProducts(UserRole.CASHIER))
    }

    @Test
    fun `unknown role has no privileged permissions`() {
        assertFalse(UserPermissions.canManageStaff(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canUpdateStoreProfile(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canAccessInventory(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canAdjustStock(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canAccessAnalytics(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canManageProducts(UserRole.UNKNOWN))
    }
}
