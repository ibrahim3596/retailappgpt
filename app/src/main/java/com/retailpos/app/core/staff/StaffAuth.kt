package com.retailpos.app.core.staff

import com.retailpos.app.core.permissions.StaffPermission
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object StaffPinPolicy {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_MILLIS = 5 * 60 * 1000L

    fun validatePin(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all(Char::isDigit)
}

object StaffPinHasher {
    private const val SALT_BYTES = 16

    fun hash(pin: String, salt: ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }): String {
        require(StaffPinPolicy.validatePin(pin)) { "PIN must contain 4 to 8 digits." }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(digest.digest())}"
    }

    fun verify(pin: String, stored: String): Boolean {
        if (!StaffPinPolicy.validatePin(pin)) return false
        val parts = stored.split(':', limit = 2)
        if (parts.size != 2) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(expected, digest.digest())
    }
}

data class StaffSession(
    val staffId: String,
    val name: String,
    val role: StaffRole,
    val signedInAt: Long
)

object StaffSessionPermissions {
    fun can(role: StaffRole, permission: StaffPermission): Boolean = StaffPermissionRules.hasPermission(role, permission)

    fun maxBillDiscountPercent(role: StaffRole): Double = StaffPermissionRules.billDiscountAuthorization(role).maxPercent
}
