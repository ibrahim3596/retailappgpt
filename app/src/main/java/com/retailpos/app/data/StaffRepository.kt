package com.retailpos.app.data

import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.staff.StaffPinHasher
import com.retailpos.app.core.staff.StaffPinPolicy
import com.retailpos.app.core.staff.StaffSession
import java.util.UUID

sealed interface StaffSignInResult {
    data class Success(val session: StaffSession) : StaffSignInResult
    data object InvalidCredentials : StaffSignInResult
    data object Locked : StaffSignInResult
    data object Inactive : StaffSignInResult
}

class StaffRepository(private val dao: StaffDao) {
    suspend fun createStaff(
        storeId: String,
        name: String,
        username: String,
        pin: String,
        role: StaffRole,
        now: Long = System.currentTimeMillis()
    ): StaffEntity {
        require(name.trim().isNotEmpty()) { "Staff name is required." }
        require(username.trim().isNotEmpty()) { "Username is required." }
        require(StaffPinPolicy.validatePin(pin)) { "PIN must contain 4 to 8 digits." }
        check(dao.findByUsername(storeId, username.trim().lowercase()) == null) { "Username already exists." }

        val staff = StaffEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            name = name.trim(),
            username = username.trim().lowercase(),
            role = role.name,
            pinHash = StaffPinHasher.hash(pin),
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(staff)
        return staff
    }

    suspend fun signIn(storeId: String, username: String, pin: String, now: Long = System.currentTimeMillis()): StaffSignInResult {
        val staff = dao.findByUsername(storeId, username.trim().lowercase()) ?: return StaffSignInResult.InvalidCredentials
        if (!staff.active) return StaffSignInResult.Inactive
        if ((staff.lockedUntil ?: 0L) > now) return StaffSignInResult.Locked

        if (StaffPinHasher.verify(pin, staff.pinHash)) {
            dao.updatePinFailures(storeId, staff.id, 0, null, now)
            val role = runCatching { StaffRole.valueOf(staff.role) }.getOrDefault(StaffRole.CASHIER)
            return StaffSignInResult.Success(StaffSession(staff.id, staff.name, role, now))
        }

        val attempts = staff.failedPinAttempts + 1
        val lock = if (attempts >= StaffPinPolicy.MAX_FAILED_ATTEMPTS) now + StaffPinPolicy.LOCKOUT_MILLIS else null
        dao.updatePinFailures(storeId, staff.id, attempts, lock, now)
        return if (lock != null) StaffSignInResult.Locked else StaffSignInResult.InvalidCredentials
    }
}
