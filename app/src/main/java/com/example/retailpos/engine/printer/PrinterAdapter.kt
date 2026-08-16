package com.example.retailpos.engine.printer

import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
import com.example.retailpos.data.local.entity.StoreEntity

sealed class PrinterStatus {
    object Connected : PrinterStatus()
    object Disconnected : PrinterStatus()
    object Printing : PrinterStatus()
    data class Error(val message: String) : PrinterStatus()
}

interface PrinterAdapter {
    suspend fun connect(targetAddress: String): PrinterStatus
    suspend fun disconnect()
    suspend fun printReceipt(
        store: StoreEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): PrinterStatus
    fun getStatus(): PrinterStatus
}

class MockThermalPrinterAdapter : PrinterAdapter {
    private var currentStatus: PrinterStatus = PrinterStatus.Disconnected

    override suspend fun connect(targetAddress: String): PrinterStatus {
        currentStatus = PrinterStatus.Connected
        return currentStatus
    }

    override suspend fun disconnect() {
        currentStatus = PrinterStatus.Disconnected
    }

    override suspend fun printReceipt(
        store: StoreEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): PrinterStatus {
        currentStatus = PrinterStatus.Printing
        val commands = EscPosReceiptGenerator.generateEscPosCommands(store, invoice, items)
        // Simulate thermal print buffer writing
        kotlinx.coroutines.delay(300)
        currentStatus = PrinterStatus.Connected
        return PrinterStatus.Connected
    }

    override fun getStatus(): PrinterStatus = currentStatus
}
