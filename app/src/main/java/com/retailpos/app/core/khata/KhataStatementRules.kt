package com.retailpos.app.core.khata

import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.CustomerLedgerEntry
import java.util.Locale

object KhataStatementRules {
    fun format(customer: CustomerEntity, balance: Double, entries: List<CustomerLedgerEntry>): String {
        val builder = StringBuilder()
        builder.append(customer.name).append('\n')
        if (customer.phone.isNotBlank()) builder.append(customer.phone).append('\n')
        builder.append("Balance: ").append(money(balance)).append("\n\n")
        if (entries.isEmpty()) builder.append("No transactions yet.\n")
        else entries.forEach { entry ->
            builder.append(entry.type.replace('_', ' '))
                .append(" — ")
                .append(money(entry.amount))
                .append(" — ")
                .append(entry.note)
                .append('\n')
        }
        return builder.toString().trim()
    }

    private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
}
