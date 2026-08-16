package com.example.retailpos.engine.sync

import android.content.Context
import com.example.retailpos.data.local.dao.InvoiceDao
import com.example.retailpos.data.local.dao.SyncDao
import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.SyncCommandEntity
import com.example.retailpos.data.local.entity.SyncConflictEntity
import com.example.retailpos.data.local.entity.SyncStatus
import java.util.UUID

sealed class SyncPushResult {
    data class Success(val commandId: String) : SyncPushResult()
    data class IdempotentDuplicate(val commandId: String) : SyncPushResult()
    data class ConflictDetected(val conflictId: String) : SyncPushResult()
    data class Error(val message: String) : SyncPushResult()
}

class SyncEngine(
    private val context: Context,
    private val invoiceDao: InvoiceDao,
    private val syncDao: SyncDao
) {

    private val installationIdManager = InstallationIdManager(context)

    suspend fun getOrCreateInstallationId(): String {
        return installationIdManager.getOrCreateInstallationId()
    }

    /**
     * Queues an offline sale command idempotently for server processing
     */
    suspend fun processPushInvoice(
        storeId: String,
        invoice: InvoiceEntity
    ): SyncPushResult {
        val installationId = getOrCreateInstallationId()
        val idempotencyKey = "SALE-${installationId}-${invoice.localId}"

        // 1. Idempotency Check
        val existingInvoices = invoiceDao.getInvoiceByLocalId(storeId, invoice.localId)
        if (existingInvoices != null && existingInvoices.syncStatus == SyncStatus.SYNCED) {
            return SyncPushResult.IdempotentDuplicate(invoice.id)
        }

        // 2. Queue Domain Command in Room Sync Queue
        val command = SyncCommandEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            installationId = installationId,
            localTransactionId = invoice.localId,
            commandType = "SALE",
            idempotencyKey = idempotencyKey,
            payloadJson = "{\"localId\":\"${invoice.localId}\",\"grandTotal\":${invoice.grandTotal},\"paymentMethod\":\"${invoice.paymentMethod.name}\"}",
            status = "PENDING"
        )
        syncDao.insertSyncCommand(command)

        // Mark local invoice as SYNCED once queued locally
        invoiceDao.insertInvoice(invoice.copy(syncStatus = SyncStatus.SYNCED))

        return SyncPushResult.Success(command.id)
    }

    suspend fun resolveConflict(conflictId: String, choice: String, syncConflict: SyncConflictEntity) {
        val updatedStatus = when (choice) {
            "SERVER_WINS" -> "RESOLVED_SERVER"
            "LOCAL_WINS" -> "RESOLVED_LOCAL"
            else -> "RESOLVED_MERGE"
        }
        syncDao.updateSyncConflict(syncConflict.copy(status = updatedStatus))
    }
}
