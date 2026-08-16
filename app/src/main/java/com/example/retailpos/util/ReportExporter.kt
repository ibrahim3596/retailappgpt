package com.example.retailpos.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
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
