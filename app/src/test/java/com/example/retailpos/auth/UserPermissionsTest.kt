package com.example.retailpos.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPermissionsTest {

    @Test
    fun `only owner can manage staff`() {
        assertTrue(UserPermissions.canManageStaff(UserRole.OWNER))
        assertFalse(UserPermissions.canManageStaff(UserRole.MANAGER))
        assertFalse(UserPermissions.canManageStaff(UserRole.CASHIER))
    }

    @Test
    fun `owner and manager can access inventory`() {
        assertTrue(UserPermissions.canAccessInventory(UserRole.OWNER))
        assertTrue(UserPermissions.canAccessInventory(UserRole.MANAGER))
        assertFalse(UserPermissions.canAccessInventory(UserRole.CASHIER))
    }

    @Test
    fun `owner and manager can adjust stock`() {
        assertTrue(UserPermissions.canAdjustStock(UserRole.OWNER))
        assertTrue(UserPermissions.canAdjustStock(UserRole.MANAGER))
        assertFalse(UserPermissions.canAdjustStock(UserRole.CASHIER))
    }

    @Test
    fun `owner and manager can manage products`() {
        assertTrue(UserPermissions.canManageProducts(UserRole.OWNER))
        assertTrue(UserPermissions.canManageProducts(UserRole.MANAGER))
        assertFalse(UserPermissions.canManageProducts(UserRole.CASHIER))
    }

    @Test
    fun `owner and manager can manage Khata`() {
        assertTrue(UserPermissions.canManageKhata(UserRole.OWNER))
        assertTrue(UserPermissions.canManageKhata(UserRole.MANAGER))
        assertFalse(UserPermissions.canManageKhata(UserRole.CASHIER))
    }

    @Test
    fun `any role can perform billing`() {
        assertTrue(UserPermissions.canPerformBilling(UserRole.OWNER))
        assertTrue(UserPermissions.canPerformBilling(UserRole.MANAGER))
        assertTrue(UserPermissions.canPerformBilling(UserRole.CASHIER))
    }

    @Test
    fun `any role can manage customers`() {
        assertTrue(UserPermissions.canManageCustomers(UserRole.OWNER))
        assertTrue(UserPermissions.canManageCustomers(UserRole.MANAGER))
        assertTrue(UserPermissions.canManageCustomers(UserRole.CASHIER))
    }

    @Test
    fun `unknown role gets restricted access`() {
        assertFalse(UserPermissions.canManageStaff(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canAccessInventory(UserRole.UNKNOWN))
        assertFalse(UserPermissions.canAdjustStock(UserRole.UNKNOWN))
    }
}
