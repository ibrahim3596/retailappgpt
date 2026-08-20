package com.retailpos.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {
    fun format(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        storeName: String = "RETAILPOS STORE"
    ): String {
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.createdAt))
        return buildString {
            appendLine(storeName)
            appendLine("SALE RECEIPT")
            appendLine("Sale: ${sale.id}")
            appendLine(date)
            appendLine("Payment: ${sale.paymentMethod}")
            appendLine("------------------------------")
            lines.forEach { line ->
                appendLine(line.name)
                appendLine("${line.quantity.clean()} ${line.unit} x ${money(line.unitPrice)} = ${money(line.lineTotal)}")
                if (line.taxAmount > 0.0) appendLine("GST ${cleanRate(line.taxRatePercent)}%: ${money(line.taxAmount)}")
            }
            appendLine("------------------------------")
            appendLine("SUBTOTAL: ${money(sale.subtotal)}")
            if (sale.discountAmount > 0.0) appendLine("DISCOUNT: -${money(sale.discountAmount)}")
            if (sale.taxAmount > 0.0) appendLine("GST: ${money(sale.taxAmount)}")
            appendLine("TOTAL: ${money(sale.total)}")
            if (sale.paymentMethod == "CASH" && sale.amountTendered != null) {
                appendLine("CASH RECEIVED: ${money(sale.amountTendered)}")
                if (sale.changeAmount > 0.0) appendLine("CHANGE: ${money(sale.changeAmount)}")
            }
        }
    }

    private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
    private fun cleanRate(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
    private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
}
