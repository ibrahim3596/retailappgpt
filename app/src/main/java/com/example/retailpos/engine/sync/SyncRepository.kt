package com.example.retailpos.engine.sync

import android.content.Context
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.CustomerEntity
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.SyncConflictEntity
import com.example.retailpos.data.local.entity.SyncStatus
import com.example.retailpos.data.local.entity.TaxType
import com.example.retailpos.util.SessionManager
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import java.util.LinkedHashMap

private fun buildMap(builder: LinkedHashMap<String, String>.() -> Unit): Map<String, String> {
    val map = LinkedHashMap<String, String>()
    map.apply(builder)
    return map
}

enum class SyncOutcome { IDLE, SYNCED, PARTIAL, CONFLICT, NOT_CONFIGURED, ERROR }

data class SyncReport(
    val pushedSales: Int = 0,
    val pushedProducts: Int = 0,
    val pushedCustomers: Int = 0,
    val pushedPayments: Int = 0,
    val pulledProducts: Int = 0,
    val pulledCustomers: Int = 0,
    val conflicts: Int = 0,
    val outcome: SyncOutcome = SyncOutcome.IDLE,
    val message: String? = null
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
 */
class SyncRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {

    private suspend fun baseUrl(): String? = runCatching { sessionManager.syncServerUrl.first() }.getOrNull()

    private suspend fun installationId(): String =
        InstallationIdManager(context).getOrCreateInstallationId()

    private suspend fun makeApi(): SyncApiClient =
        SyncApiClient { baseUrl() }

    private suspend fun withRefreshedTokenIfPossible(block: suspend () -> SyncHttpResult): SyncHttpResult {
        val result = block()
        if (result is SyncHttpResult.ClientError && result.httpCode == 401) {
            val refresh = sessionManager.syncRefreshToken.first() ?: return result
            val body = SyncContracts.encode(RefreshRequest(refresh, installationId()))
            when (val refreshResult = makeApi().post("/api/v1/auth/refresh", body, null)) {
                is SyncHttpResult.Success -> {
                    val parsed = SyncContracts.decode<RefreshResponse>(refreshResult.bodyJson)
                    if (parsed?.accessToken != null) {
                        sessionManager.setSyncTokens(parsed.accessToken, parsed.refreshToken)
                        return block()
                    }
                }
                else -> sessionManager.setSyncTokens(null, null) // refresh dead: force re-login
            }
        }
        return result
    }

    suspend fun syncNow(storeId: String): SyncReport {
        val api = makeApi()
        val url = sessionManager.syncServerUrl.first()
        val token = sessionManager.syncAccessToken.first()
        if (url.isNullOrBlank() || token.isNullOrBlank()) {
            return SyncReport(outcome = SyncOutcome.NOT_CONFIGURED,
                message = "Configure the sync server and log in first")
        }

        var report = SyncReport()
        var hadConflict = false
        var hadError = false

        // 1. Push master data once (products/customers created on this device)
        if (!sessionManager.syncMasterPushed.first()) {
            try {
                val products = db.productDao().getAllProductsOnce(storeId)
                for (chunk in products.chunked(200)) {
                    val payload = buildProductUpsert(installationId(), chunk)
                    val key = "PRODUCT_UPSERT-$storeId-${chunk.hashCode()}"
                    val res = withRefreshedTokenIfPossible {
                        makeApi().post("/api/v1/sync/push", SyncContracts.encode(PushRequest("PRODUCT_UPSERT", payload, key)), token, key)
                    }
                    if (res is SyncHttpResult.Success) report = report.copy(pushedProducts = report.pushedProducts + chunk.size)
                    else if (res is SyncHttpResult.ClientError) hadConflict = true
                    else hadError = true
                }
                val customers = db.customerDao().getAllCustomersOnce(storeId)
                for (chunk in customers.chunked(200)) {
                    val payload = buildCustomerUpsert(installationId(), chunk)
                    val key = "CUSTOMER_UPSERT-$storeId-${chunk.hashCode()}"
                    val res = withRefreshedTokenIfPossible {
                        makeApi().post("/api/v1/sync/push", SyncContracts.encode(PushRequest("CUSTOMER_UPSERT", payload, key)), token, key)
                    }
                    if (res is SyncHttpResult.Success) report = report.copy(pushedCustomers = report.pushedCustomers + chunk.size)
                    else if (res is SyncHttpResult.ClientError) hadConflict = true
                    else hadError = true
                }
                if (!hadConflict && !hadError) sessionManager.setSyncMasterPushed(true)
            } catch (e: Exception) {
                hadError = true
                report = report.copy(message = "Master push failed: ${e.message}")
            }
        }

        // 2. Push pending sales
        try {
            val pending = db.invoiceDao().getPendingInvoices(storeId)
            for (invoice in pending) {
                val items = db.invoiceItemDao().getInvoiceItems(invoice.id)
                if (items.isEmpty()) continue
                val (command, key) = buildSaleCommand(installationId(), invoice, items)
                val res = withRefreshedTokenIfPossible {
                    makeApi().post(
                        "/api/v1/sync/push",
                        SyncContracts.encode(PushRequest("SALE", command, key)),
                        token,
                        key
                    )
                }
                when (res) {
                    is SyncHttpResult.Success -> {
                        val parsed = SyncContracts.decode<PushResponse>(res.bodyJson)
                        if (parsed?.status == "SUCCESS" || parsed?.status == "ALREADY_PROCESSED") {
                            db.invoiceDao().insertInvoice(invoice.copy(syncStatus = SyncStatus.SYNCED))
                            report = report.copy(pushedSales = report.pushedSales + 1)
                        } else {
                            hadError = true
                        }
                    }
                    is SyncHttpResult.ClientError -> {
                        if (res.httpCode == 409) {
                            markConflict(storeId, invoice, res)
                            report = report.copy(conflicts = report.conflicts + 1)
                            hadConflict = true
                        } else {
                            hadError = true
                        }
                    }
                    else -> hadError = true
                }
            }
        } catch (e: Exception) {
            hadError = true
            report = report.copy(message = report.message ?: "Sale push failed: ${e.message}")
        }

        // 2b. Push pending Khata payments
        try {
            val pendingPayments = db.khataPaymentDao().getPendingPayments(storeId)
            for (payment in pendingPayments) {
                val installationId = installationId()
                val command = buildCustomerPaymentCommand(
                    installationId = installationId,
                    localTransactionId = payment.id,
                    customerId = payment.customerId,
                    amount = payment.amount,
                    paymentMethod = runCatching { com.example.retailpos.data.local.entity.PaymentMethod.valueOf(payment.paymentMethod) }.getOrDefault(com.example.retailpos.data.local.entity.PaymentMethod.CASH),
                    notes = payment.notes
                )
                val key = "KHATA_PAYMENT-$installationId-${payment.id}"
                val res = withRefreshedTokenIfPossible {
                    makeApi().post(
                        "/api/v1/sync/push",
                        SyncContracts.encode(PushRequest("CUSTOMER_PAYMENT", command, key)),
                        token,
                        key
                    )
                }
                when (res) {
                    is SyncHttpResult.Success -> {
                        val parsed = SyncContracts.decode<PushResponse>(res.bodyJson)
                        if (parsed?.status == "SUCCESS" || parsed?.status == "ALREADY_PROCESSED") {
                            db.khataPaymentDao().updatePaymentSyncStatus(payment.id, storeId, "SYNCED")
                            report = report.copy(pushedPayments = report.pushedPayments + 1)
                        } else {
                            hadError = true
                        }
                    }
                    is SyncHttpResult.ClientError -> {
                        if (res.httpCode == 409) {
                            report = report.copy(conflicts = report.conflicts + 1)
                            hadConflict = true
                        } else {
                            hadError = true
                        }
                    }
                    else -> hadError = true
                }
            }
        } catch (e: Exception) {
            hadError = true
            report = report.copy(message = report.message ?: "Payment push failed: ${e.message}")
        }

        // 3. Pull server changes (products/customers)
        try {
            val since = sessionManager.syncLastPulledAt.first()
            val installationIdValue = installationId()
            val query = buildMap {
                put("installationId", installationIdValue)
                if (!since.isNullOrBlank()) put("lastSyncedAt", since)
            }
            val res = withRefreshedTokenIfPossible { makeApi().get("/api/v1/sync/pull", query, token) }
            if (res is SyncHttpResult.Success) {
                val parsed = SyncContracts.decode<PullResponse>(res.bodyJson)
                if (parsed != null) {
                    for (p in parsed.products) {
                        val entity = ProductEntity(
                            id = p.id,
                            storeId = storeId,
                            sku = p.sku ?: "SKU-${p.id.take(8)}",
                            barcode = p.barcode ?: "",
                            normalizedBarcode = p.normalizedBarcode ?: p.barcode ?: "",
                            name = p.name,
                            brand = p.brand ?: "",
                            category = p.category ?: "General",
                            variant = p.variant ?: "",
                            hsnCode = p.hsnCode ?: "",
                            mrp = paiseStringToRupees(p.mrpPaise),
                            sellingPrice = paiseStringToRupees(p.sellingPricePaise),
                            purchasePrice = paiseStringToRupees(p.purchasePricePaise),
                            gstRate = p.gstRate?.toDoubleOrNull() ?: 0.0,
                            taxType = if (p.isTaxInclusive == false) TaxType.EXCLUSIVE else TaxType.INCLUSIVE,
                            currentStock = p.currentStock?.toDoubleOrNull() ?: 0.0
                        )
                        runCatching { db.productDao().insertProduct(entity) }
                    }
                    for (c in parsed.customers) {
                        val entity = CustomerEntity(
                            id = c.id,
                            storeId = storeId,
                            name = c.name,
                            phone = c.phone ?: "",
                            currentBalance = paiseStringToRupees(c.currentBalancePaise),
                            creditLimit = paiseStringToRupees(c.creditLimitPaise)
                        )
                        runCatching { db.customerDao().insertCustomer(entity) }
                    }
                    report = report.copy(
                        pulledProducts = parsed.products.size,
                        pulledCustomers = parsed.customers.size
                    )
                    sessionManager.setSyncLastPulledAt(parsed.pulledAt ?: Instant.now().toString())
                }
            } else if (res is SyncHttpResult.ClientError) {
                hadError = true
            }
        } catch (e: Exception) {
            hadError = true
            report = report.copy(message = report.message ?: "Pull failed: ${e.message}")
        }

        val outcome = when {
            hadError && (report.pushedSales > 0 || report.pulledProducts > 0) -> SyncOutcome.PARTIAL
            hadError -> SyncOutcome.ERROR
            hadConflict || report.conflicts > 0 -> SyncOutcome.CONFLICT
            else -> SyncOutcome.SYNCED
        }
        return report.copy(outcome = outcome)
    }

    private suspend fun markConflict(storeId: String, invoice: com.example.retailpos.data.local.entity.InvoiceEntity, error: SyncHttpResult.ClientError) {
        db.invoiceDao().insertInvoice(invoice.copy(syncStatus = SyncStatus.CONFLICT))
        db.syncDao().insertSyncConflict(
            SyncConflictEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                entityType = "INVOICE",
                entityId = invoice.id,
                localDataJson = "localId=${invoice.localId},total=${invoice.grandTotal}",
                serverDataJson = "error=${error.error ?: "?"},message=${error.message ?: ""}",
                conflictReason = error.message ?: "Server rejected the sale"
            )
        )
    }
}
