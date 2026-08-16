package com.example.retailpos.engine.printer

import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
import com.example.retailpos.data.local.entity.StoreEntity
import java.text.SimpleDateFormat
import java.util.*

object EscPosReceiptGenerator {

    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())

    fun generateTextReceipt(
        store: StoreEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): String {
        val sb = StringBuilder()
        val line = "------------------------------------------------\n"

        sb.append("                  TAX INVOICE                   \n")
        sb.append("               ${store.name.uppercase()}               \n")
        if (store.address.isNotEmpty()) sb.append("        ${store.address}        \n")
        if (store.phone.isNotEmpty()) sb.append("               Ph: ${store.phone}               \n")
        if (store.gstin.isNotEmpty()) sb.append("            GSTIN: ${store.gstin}            \n")
        sb.append(line)

        sb.append("Invoice No: ${invoice.invoiceNumber}\n")
        sb.append("Date      : ${dateFormat.format(Date(invoice.createdAt))}\n")
        sb.append("Customer  : ${invoice.customerName} (${invoice.customerPhone.ifEmpty { "N/A" }})\n")
        sb.append("Payment   : ${invoice.paymentMethod.name}\n")
        sb.append(line)

        sb.append(String.format("%-20s %4s %8s %8s\n", "ITEM", "QTY", "RATE", "TOTAL"))
        sb.append(line)

        for (item in items) {
            val nameTrunc = if (item.productName.length > 20) item.productName.substring(0, 18) + ".." else item.productName
            sb.append(String.format("%-20s %4.0f %8.2f %8.2f\n", nameTrunc, item.quantity, item.sellingPrice, item.itemTotal))
            if (item.gstRate > 0) {
                sb.append("  [HSN: ${item.hsnCode.ifEmpty { "N/A" }} | GST: ${item.gstRate.toInt()}%]\n")
            }
        }
        sb.append(line)

        sb.append(String.format("%-32s ₹%8.2f\n", "SUBTOTAL:", invoice.subtotal))
        if (invoice.discount > 0) {
            sb.append(String.format("%-32s-₹%8.2f\n", "DISCOUNT:", invoice.discount))
        }
        if (invoice.isInterstate) {
            sb.append(String.format("%-32s ₹%8.2f\n", "IGST TOTAL:", invoice.igstTotal))
        } else {
            sb.append(String.format("%-32s ₹%8.2f\n", "CGST TOTAL:", invoice.cgstTotal))
            sb.append(String.format("%-32s ₹%8.2f\n", "SGST TOTAL:", invoice.sgstTotal))
        }
        sb.append(String.format("%-32s ₹%8.2f\n", "TOTAL TAX:", invoice.totalGst))
        sb.append(line)
        sb.append(String.format("%-32s ₹%8.2f\n", "GRAND TOTAL:", invoice.grandTotal))
        sb.append(line)

        if (invoice.paymentMethod.name == "CASH") {
            sb.append(String.format("%-32s ₹%8.2f\n", "CASH RECEIVED:", invoice.amountReceived))
            sb.append(String.format("%-32s ₹%8.2f\n", "CHANGE DUE:", invoice.changeDue))
            sb.append(line)
        }

        sb.append("          Thank you for shopping with us!       \n")
        sb.append("           Computer Generated Invoice           \n\n\n")

        return sb.toString()
    }

    fun generateEscPosCommands(
        store: StoreEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // ESC @ Init printer
        bytes.addAll(byteArrayOf(0x1B, 0x40).toList())
        // ESC a 1 Align Center
        bytes.addAll(byteArrayOf(0x1B, 0x61, 0x01).toList())
        // ESC ! 0x10 Double Height / Bold
        bytes.addAll(byteArrayOf(0x1B, 0x21, 0x10).toList())

        val text = generateTextReceipt(store, invoice, items)
        for (char in text) {
            bytes.add(char.code.toByte())
        }

        // Cut paper command GS V 66 0
        bytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())

        return bytes.toByteArray()
    }
}
