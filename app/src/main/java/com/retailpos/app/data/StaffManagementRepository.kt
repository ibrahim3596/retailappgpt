package com.retailpos.app.data

import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.staff.StaffManagementRules
import com.retailpos.app.core.staff.StaffPinHasher
import com.retailpos.app.core.staff.StaffPinPolicy

class StaffManagementRepository(private val dao: StaffDao) {
    suspend fun list(storeId: String): List<StaffEntity> = dao.list(storeId)

    suspend fun setActive(storeId: String, staffId: String, active: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        if (!active) {
            val member = dao.getById(storeId, staffId) ?: return false
            val role = runCatching { StaffRole.valueOf(member.role) }.getOrNull() ?: return false
            if (!StaffManagementRules.canDeactivate(role, member.active, active, dao.countActiveOwners(storeId))) return false
        }
        return dao.setActive(storeId, staffId, active, now) == 1
    }

    suspend fun changePin(storeId: String, staffId: String, newPin: String, now: Long = System.currentTimeMillis()): Boolean {
        require(StaffPinPolicy.validatePin(newPin)) { "PIN must contain 4 to 8 digits." }
        val hash = StaffPinHasher.hash(newPin)
        return dao.updatePinHash(storeId, staffId, hash, now) == 1
    }

    suspend fun changeRole(storeId: String, staffId: String, role: StaffRole, now: Long = System.currentTimeMillis()): Boolean {
        val member = dao.getById(storeId, staffId) ?: return false
        val currentRole = runCatching { StaffRole.valueOf(member.role) }.getOrNull() ?: return false
        if (!StaffManagementRules.canChangeRole(currentRole, member.active, role, dao.countActiveOwners(storeId))) return false
        return dao.updateRole(storeId, staffId, role.name, now) == 1
    }
}
