package com.retailpos.app.data

import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.staff.StaffPinHasher
import com.retailpos.app.core.staff.StaffPinPolicy

class StaffManagementRepository(private val dao: StaffDao) {
    suspend fun list(storeId: String): List<StaffEntity> = dao.list(storeId)

    suspend fun setActive(storeId: String, staffId: String, active: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        if (!active) {
            val member = dao.getById(storeId, staffId) ?: return false
            if (member.active && member.role == StaffRole.OWNER && dao.countActiveOwners(storeId) <= 1) return false
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
        if (member.role == StaffRole.OWNER && role != StaffRole.OWNER && member.active && dao.countActiveOwners(storeId) <= 1) return false
        return dao.updateRole(storeId, staffId, role.name, now) == 1
    }
}
