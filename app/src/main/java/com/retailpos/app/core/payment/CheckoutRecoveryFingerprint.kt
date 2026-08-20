package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Stable fingerprints for recovering a cart and for idempotent checkout requests. */
object CheckoutRecoveryFingerprint {
    fun of(lines: List<CartLine>): String = hash(canonicalCart(lines))

    fun transactionOf(
        lines: List<CartLine>,
        paymentMethod: String,
        customerId: String?,
        billDiscountAmount: Double
    ): String = hash(
        canonicalCart(lines) + "\nTRANSACTION|$paymentMethod|${customerId.orEmpty()}|${format(billDiscountAmount)}"
    )

    private fun canonicalCart(lines: List<CartLine>): String = lines
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

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(Locale.US, byte.toInt() and 0xff) }
    }

    private fun format(value: Double): String = java.lang.Double.toString(value)
}
