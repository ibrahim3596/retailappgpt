package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Stable fingerprint tying recoverable checkout state to the exact cart/pricing snapshot. */
object CheckoutRecoveryFingerprint {
    fun of(lines: List<CartLine>): String {
        val canonical = lines
            .sortedBy { it.productId }
            .joinToString("\n") { line ->
                listOf(
                    line.productId,
                    line.name,
                    line.sku.orEmpty(),
                    line.unit,
                    format(line.unitPrice),
                    format(line.quantity),
                    line.overrideUnitPrice?.let(::format).orEmpty(),
                    format(line.itemDiscountAmount)
                ).joinToString("|")
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(Locale.US, byte.toInt() and 0xff) }
    }

    private fun format(value: Double): String = java.lang.Double.toString(value)
}
