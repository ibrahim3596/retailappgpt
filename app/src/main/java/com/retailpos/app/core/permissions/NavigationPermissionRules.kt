package com.retailpos.app.core.permissions

import com.retailpos.app.core.staff.StaffRole

object NavigationPermissionRules {
    fun canOpenProducts(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.MANAGE_PRODUCTS)

    fun canOpenInventory(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.ADJUST_INVENTORY)

    fun canOpenAnalytics(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.VIEW_REPORTS)

    fun canOpenSettings(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.CHANGE_STORE_SETTINGS)

    fun canOpenStaffManagement(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.MANAGE_STAFF)

    fun canManageExpenses(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.MANAGE_EXPENSES)

    fun canProcessReturns(role: StaffRole): Boolean =
        StaffPermissionRules.hasPermission(role, StaffPermission.PROCESS_RETURN)
}
