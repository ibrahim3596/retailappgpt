package com.example.retailpos.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.retailpos.data.local.entity.ExpenseEntity
import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.PurchaseEntity
import com.example.retailpos.data.local.entity.PurchaseItemEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportInvoicesToCsv(context: Context, invoices: List<InvoiceEntity>): File? {
        val fileName = "Sales_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            file.bufferedWriter().use { out ->
                // Header
                out.write("Date,Invoice #,Customer,Payment,Subtotal,GST,Discount,Total\n")

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                invoices.forEach { inv ->
                    val dateStr = sdf.format(Date(inv.createdAt))
                    out.write("\"$dateStr\",\"${inv.invoiceNumber}\",\"${inv.customerName}\",\"${inv.paymentMethod}\",${inv.subtotal},${inv.totalGst},${inv.discount},${inv.grandTotal}\n")
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportGstReportToCsv(context: Context, invoices: List<InvoiceEntity>): File? {
        val fileName = "GST_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            file.bufferedWriter().use { out ->
                // Header
                out.write("Date,Invoice #,Taxable Value,CGST,SGST,IGST,Total GST,Grand Total\n")

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                invoices.forEach { inv ->
                    val dateStr = sdf.format(Date(inv.createdAt))
                    out.write("\"$dateStr\",\"${inv.invoiceNumber}\",${inv.subtotal},${inv.cgstTotal},${inv.sgstTotal},${inv.igstTotal},${inv.totalGst},${inv.grandTotal}\n")
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportProductsToCsv(context: Context, products: List<ProductEntity>): File? {
        val fileName = "Products_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            file.bufferedWriter().use { out ->
                out.write("SKU,Barcode,Name,Brand,Category,Variant,MRP,Selling Price,Purchase Price,Stock,Min Stock,GST Rate,Tax Type\n")
                products.forEach { p ->
                    out.write("\"${p.sku}\",\"${p.barcode}\",\"${p.name}\",\"${p.brand}\",\"${p.category}\",\"${p.variant}\",${p.mrp},${p.sellingPrice},${p.purchasePrice},${p.currentStock},${p.minStock},${p.gstRate},${p.taxType}\n")
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportExpensesToCsv(context: Context, expenses: List<ExpenseEntity>): File? {
        val fileName = "Expenses_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            file.bufferedWriter().use { out ->
                out.write("Date,Category,Amount,Payment Method,Notes\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                expenses.forEach { e ->
                    val dateStr = sdf.format(Date(e.date))
                    out.write("\"$dateStr\",\"${e.category}\",${e.amount},\"${e.paymentMethod}\",\"${e.notes}\"\n")
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportPurchasesToCsv(context: Context, purchases: List<PurchaseEntity>): File? {
        val fileName = "Purchases_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            file.bufferedWriter().use { out ->
                out.write("Date,PO Number,Supplier,Items Count,Total Amount,GST,Notes\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                purchases.forEach { p ->
                    val dateStr = sdf.format(Date(p.createdAt))
                    out.write("\"$dateStr\",\"${p.invoiceNumber}\",\"${p.supplierName}\",${p.items.size},${p.totalAmount},${p.gstTotal},\"${p.notes}\"\n")
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}