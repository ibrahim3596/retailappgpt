package com.retailpos.app.core.staff

import com.retailpos.app.core.permissions.StaffRole

object StaffManagementRules {
    fun canDeactivate(role: StaffRole, active: Boolean, requestedActive: Boolean, activeOwnerCount: Int): Boolean =
        requestedActive || !(active && role == StaffRole.OWNER && activeOwnerCount <= 1)

    fun canChangeRole(role: StaffRole, active: Boolean, requestedRole: StaffRole, activeOwnerCount: Int): Boolean =
        requestedRole == StaffRole.OWNER || !(active && role == StaffRole.OWNER && activeOwnerCount <= 1)
}
