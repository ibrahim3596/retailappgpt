package com.retailpos.app.data

/** UI-ready view of a sale line and the quantity that remains refundable. */
data class ReturnCandidateLine(
    val saleLine: SaleLineEntity,
    val alreadyReturnedQuantity: Double
) {
    val remainingQuantity: Double
        get() = (saleLine.quantity - alreadyReturnedQuantity).coerceAtLeast(0.0)
}
