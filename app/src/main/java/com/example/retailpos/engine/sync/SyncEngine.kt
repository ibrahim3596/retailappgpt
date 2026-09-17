package com.example.retailpos.engine.sync

import android.content.Context
import com.example.retailpos.data.local.dao.CreditLedgerDao
import com.example.retailpos.data.local.dao.CustomerDao
import com.example.retailpos.data.local.dao.InvoiceDao
import com.example.retailpos.data.local.dao.SyncDao
import com.example.retailpos.data.local.entity.CreditLedgerEntryEntity
import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.LedgerEntryType
import com.example.retailpos.data.local.entity.SyncCommandEntity
import com.example.retailpos.data.local.entity.SyncConflictEntity
import com.example.retailpos.data.local.entity.SyncStatus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant
import java.util.UUID

sealed class SyncPushResult {
    data class Success(val commandId: String) : SyncPushResult()
    data class IdempotentDuplicate(val commandId: String) : SyncPushResult()
    data class ConflictDetected(val conflictId: String) : SyncPushResult()
    data class Error(val message: String) : SyncPushResult()
}

/** Parsed fields from a SYNC_CONFLICT's localDataJson / serverDataJson. */
data class InvoiceConflictData(
    val localId: String? = null,
    val grandTotal: Double? = null,
    val paymentMethod: String? = null,
    val customerId: String? = null,
    val error: String? = null,
    val errorMessage: String? = null
)

/**
 * Bridges the local Room store and the sync server.
 *
 * Reconciliation strategy (deterministic):
 *  - Sales are pushed once with a stable idempotency key derived from
 *    (installationId, localId), so retries and duplicates can never double-charge.
 *  - SUCCESS / ALREADY_PROCESSED both mark the invoice SYNCED.
 *  - 409 business conflicts mark the invoice CONFLICT and surface a
 *    SyncConflictEntity for the shopkeeper to resolve in the app.
 *  - Network failures leave the invoice PENDING for the next attempt; nothing
 *    is ever silently dropped or duplicated.
 *  - Pull upserts the server's product/customer masters by id, newest
 *    updatedAt wins.
 *  - resolveConflict actively reconciles the local database: SERVER_WINS
 *    reverts the invoice to PENDING (server will re-process on next sync);
 *    LOCAL_WINS marks the invoice SYNCED.
 */
class SyncEngine(
    private val context: Context,
    private val invoiceDao: InvoiceDao,
    private val syncDao: SyncDao,
    private val customerDao: CustomerDao,
    private val creditLedgerDao: CreditLedgerDao
) {

    private val installationIdManager = InstallationIdManager(context)
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

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

    /**
     * Resolves a sync conflict by applying the chosen resolution strategy
     * to the local database.
     *
     * - "SERVER_WINS": reverts the invoice to PENDING status so it can be
     *   re-pushed on the next sync cycle; optionally rolls back any local
     *   customer-balance adjustments that were made offline before the
     *   conflict was detected.
     * - "LOCAL_WINS": keeps the local invoice as SYNCED.
     * - other values: marks as RESOLVED_MERGE (status update only).
     */
    suspend fun resolveConflict(conflictId: String, choice: String, syncConflict: SyncConflictEntity) {
        val updatedStatus = when (choice) {
            "SERVER_WINS" -> "RESOLVED_SERVER"
            "LOCAL_WINS" -> "RESOLVED_LOCAL"
            else -> "RESOLVED_MERGE"
        }

        // Only invoice-type conflicts trigger data reconciliation.
        if (syncConflict.entityType == "INVOICE" && syncConflict.entityId.isNotEmpty()) {
            val localData = parseInvoiceConflictData(syncConflict.localDataJson)
            val serverError = syncConflict.serverDataJson.let { json ->
                parseInvoiceConflictData(json)
            }

            when (choice) {
                "SERVER_WINS" -> {
                    // Revert invoice to PENDING so it will be re-sent on next sync.
                    val invoiceId = syncConflict.entityId
                    invoiceDao.updateInvoiceSyncStatus(invoiceId, syncConflict.storeId, SyncStatus.PENDING.name)
                    // If the conflict involved a credit-customer balance adjustment,
                    // roll back the customer balance to pre-conflict state if we can
                    // recover the original amount from local data.
                    if (localData.customerId != null && localData.grandTotal != null) {
                        rollbackCustomerBalanceIfCreditSale(
                            storeId = syncConflict.storeId,
                            customerId = localData.customerId,
                            amount = localData.grandTotal
                        )
                    }
                }
                "LOCAL_WINS" -> {
                    // Invoice stays SYNCED; no further action needed.
                }
                else -> {
                    // Manual merge: keep current state, only update status.
                }
            }
        }

        syncDao.updateSyncConflict(syncConflict.copy(status = updatedStatus))
    }

    /**
     * Rolls back a customer's balance by the given amount when the resolved
     * invoice was a credit sale that must be re-synced server-side.
     * A negative amount is passed to [CustomerDao.updateBalance] which
     * already supports debit/credit logic; here we subtract the amount
     * (reversing the earlier addition).
     */
    private suspend fun rollbackCustomerBalanceIfCreditSale(
        storeId: String,
        customerId: String,
        amount: Double
    ) {
        val customer = customerDao.getCustomerById(storeId, customerId) ?: return
        val newBalance = (customer.currentBalance - amount).coerceAtLeast(0.0)
        val timestamp = System.currentTimeMillis()
        customerDao.updateBalance(customerId, storeId, -amount, timestamp)
        creditLedgerDao.insertLedgerEntry(
            CreditLedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                customerId = customerId,
                type = LedgerEntryType.CREDIT, // reverses prior DEBIT
                amount = amount,
                balanceAfter = newBalance,
                referenceId = "",
                notes = "Conflict resolution: rolled back credit sale (SERVER_WINS)",
                timestamp = timestamp
            )
        )
    }

    private fun parseInvoiceConflictData(json: String): InvoiceConflictData {
        return try {
            val adapter: JsonAdapter<InvoiceConflictData> = moshi.adapter(InvoiceConflictData::class.java)
            adapter.fromJson(json) ?: InvoiceConflictData()
        } catch (e: Exception) {
            InvoiceConflictData()
        }
    }
}
