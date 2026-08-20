package com.retailpos.app.core.staff

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object StaffPinPolicy {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_MILLIS = 5 * 60 * 1000L

    fun validatePin(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all(Char::isDigit)
}

object StaffPinHasher {
    private const val SALT_BYTES = 16

    fun hash(pin: String, salt: ByteArray = randomSalt()): String {
        require(StaffPinPolicy.validatePin(pin)) { "PIN must contain 4 to 8 digits." }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        val encodedSalt = Base64.getEncoder().encodeToString(salt)
        val encodedHash = Base64.getEncoder().encodeToString(digest.digest())
        return "$encodedSalt:$encodedHash"
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

    private fun randomSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
}

data class StaffSession(
    val staffId: String,
    val name: String,
    val role: StaffRole,
    val signedInAt: Long
)

enum class StaffRole {
    OWNER,
    MANAGER,
    CASHIER
}

object StaffPermissionRules {
    fun can(role: StaffRole, permission: StaffPermission): Boolean = when (permission) {
        StaffPermission.BILL_DISCOUNT -> true
        StaffPermission.ITEM_DISCOUNT -> role != StaffRole.CASHIER
        StaffPermission.PRICE_OVERRIDE -> role != StaffRole.CASHIER
        StaffPermission.VOID_BILL -> role != StaffRole.CASHIER
        StaffPermission.RETURN_SALE -> role != StaffRole.CASHIER
        StaffPermission.VIEW_REPORTS -> true
        StaffPermission.MANAGE_PRODUCTS -> role != StaffRole.CASHIER
        StaffPermission.MANAGE_STAFF -> role == StaffRole.OWNER
        StaffPermission.MANAGE_SETTINGS -> role != StaffRole.CASHIER
    }

    fun maxBillDiscountPercent(role: StaffRole): Double = when (role) {
        StaffRole.OWNER -> 100.0
        StaffRole.MANAGER -> 50.0
        StaffRole.CASHIER -> 10.0
    }
}

enum class StaffPermission {
    BILL_DISCOUNT,
    ITEM_DISCOUNT,
    PRICE_OVERRIDE,
    VOID_BILL,
    RETURN_SALE,
    VIEW_REPORTS,
    MANAGE_PRODUCTS,
    MANAGE_STAFF,
    MANAGE_SETTINGS
}
