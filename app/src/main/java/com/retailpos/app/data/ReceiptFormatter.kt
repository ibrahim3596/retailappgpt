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
            }
            appendLine("------------------------------")
            appendLine("TOTAL: ${money(sale.total)}")
        }
    }

    private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
    private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
}
