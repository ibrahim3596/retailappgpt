package com.example.retailpos.engine.sync

import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
import com.example.retailpos.data.local.entity.PaymentMethod
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.CustomerEntity
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// ---------------------------------------------------------------------------
// Wire contracts mirroring server/src/contracts/schemas.ts. All money moves as
// integer paise strings to avoid float drift between devices and the server.
// ---------------------------------------------------------------------------

data class SaleItemPayload(
    val productId: String,
    val quantity: Double
)

data class SaleCommandPayload(
    val installationId: String,
    val localTransactionId: String,
    val customerId: String? = null,
    val items: List<SaleItemPayload>,
    val paymentMethod: String,
    val amountReceivedPaise: String,
    val discountPaise: String,
    val isInterstate: Boolean,
    val clientTimestamp: String
)

data class ProductUpsertPayload(
    val products: List<ProductMasterPayload>
)

data class ProductMasterPayload(
    val id: String,
    val sku: String,
    val barcode: String? = null,
    val normalizedBarcode: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val variant: String? = null,
    val hsnCode: String? = null,
    val unit: String? = null,
    val mrpPaise: String,
    val sellingPricePaise: String,
    val purchasePricePaise: String,
    val gstRate: Double,
    val isTaxInclusive: Boolean,
    val currentStock: Double,
    val minStock: Double? = null,
    val taxType: String? = null,
    val updatedAt: Long? = null
)

data class CustomerUpsertPayload(
    val customers: List<CustomerMasterPayload>
)

data class CustomerMasterPayload(
    val id: String,
    val name: String,
    val phone: String? = null,
    val currentBalancePaise: String,
    val creditLimitPaise: String,
    val updatedAt: Long? = null
)

data class CustomerPaymentPayload(
    val installationId: String,
    val localTransactionId: String,
    val customerId: String,
    val amountPaise: String,
    val paymentMethod: String,
    val notes: String? = null
)

data class ExpensePushPayload(
    val installationId: String,
    val localTransactionId: String,
    val localId: String,
    val category: String,
    val amountPaise: String,
    val date: String?,
    val paymentMethod: String,
    val notes: String? = null
)

data class PushRequest(
    val commandType: String,
    val payload: Any,
    val idempotencyKey: String? = null
)

data class PushResponse(
    val status: String? = null,
    val message: String? = null,
    val error: String? = null,
    val officialInvoiceNumber: String? = null,
    val invoiceId: String? = null,
    val grandTotalPaise: String? = null,
    val newBalancePaise: String? = null
)

data class LoginRequest(
    val storeId: String,
    val username: String,
    val password: String,
    val installationId: String
)

data class LoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val storeId: String? = null,
    val username: String? = null,
    val role: String? = null,
    val message: String? = null
)

data class RefreshRequest(
    val refreshToken: String,
    val installationId: String
)

data class RefreshResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null
)

data class ServerProduct(
    val id: String,
    val sku: String? = null,
    val barcode: String? = null,
    val normalizedBarcode: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val variant: String? = null,
    val hsnCode: String? = null,
    val mrpPaise: String? = null,
    val sellingPricePaise: String? = null,
    val purchasePricePaise: String? = null,
    val gstRate: String? = null,
    val isTaxInclusive: Boolean? = null,
    val currentStock: String? = null,
    val version: Int? = null,
    val updatedAt: String? = null
)

data class ServerCustomer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val currentBalancePaise: String? = null,
    val creditLimitPaise: String? = null,
    val version: Int? = null,
    val updatedAt: String? = null
)

data class PullResponse(
    val products: List<ServerProduct> = emptyList(),
    val customers: List<ServerCustomer> = emptyList(),
    val hasMore: Boolean = false,
    val pulledAt: String? = null
)

object SyncContracts {

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun encode(value: Any): String {
        val adapter = moshi.adapter<Any>(value.javaClass)
        return adapter.toJson(value)
    }

    inline fun <reified T : Any> decode(json: String): T? =
        moshi.adapter<T>(T::class.java).fromJson(json)

    const val PAYMENT_CASH = "CASH"
    const val PAYMENT_UPI = "UPI"
    const val PAYMENT_CARD = "CARD"
    const val PAYMENT_CREDIT = "CREDIT"
}

// ---------------------------------------------------------------------------
// Pure mapping helpers (unit-tested). Rupees stay on-device only; the wire and
// the server speak integer paise exclusively.
// ---------------------------------------------------------------------------

fun rupeesToPaiseString(rupees: Double): String =
    Math.round(rupees * 100.0).toString()

fun paiseStringToRupees(paise: String?): Double =
    paise?.trim()?.toDoubleOrNull()?.let { it / 100.0 } ?: 0.0

fun paymentMethodToWire(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> SyncContracts.PAYMENT_CASH
    PaymentMethod.UPI -> SyncContracts.PAYMENT_UPI
    PaymentMethod.CARD -> SyncContracts.PAYMENT_CARD
    PaymentMethod.CREDIT -> SyncContracts.PAYMENT_CREDIT
}

data class SaleCommandAndKey(
    val command: SaleCommandPayload,
    val idempotencyKey: String
)

/** Builds the server SALE command from a local invoice. Pure function. */
fun buildSaleCommand(
    installationId: String,
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>
): SaleCommandAndKey {
    require(items.isNotEmpty()) { "Invoice ${invoice.localId} has no line items" }
    val command = SaleCommandPayload(
        installationId = installationId,
        localTransactionId = invoice.localId,
        customerId = invoice.customerId,
        items = items
            .groupBy { it.productId }
            .map { (productId, lines) -> SaleItemPayload(productId, lines.sumOf { it.quantity }) },
        paymentMethod = paymentMethodToWire(invoice.paymentMethod),
        amountReceivedPaise = rupeesToPaiseString(invoice.amountReceived),
        discountPaise = rupeesToPaiseString(invoice.discount),
        isInterstate = invoice.isInterstate,
        clientTimestamp = java.time.Instant.ofEpochMilli(invoice.createdAt).toString()
    )
    return SaleCommandAndKey(
        command = command,
        idempotencyKey = "SALE-$installationId-${invoice.localId}"
    )
}

/** Builds the server PRODUCT_UPSERT command from local master data. */
fun buildProductUpsert(installationId: String, products: List<ProductEntity>): ProductUpsertPayload =
    ProductUpsertPayload(
        products = products.map { p ->
            ProductMasterPayload(
                id = p.id,
                sku = p.sku,
                barcode = p.barcode,
                normalizedBarcode = p.normalizedBarcode,
                name = p.name,
                brand = p.brand,
                category = p.category,
                variant = p.variant,
                hsnCode = p.hsnCode,
                unit = p.unit,
                mrpPaise = rupeesToPaiseString(p.mrp),
                sellingPricePaise = rupeesToPaiseString(p.sellingPrice),
                purchasePricePaise = rupeesToPaiseString(p.purchasePrice),
                gstRate = p.gstRate,
                isTaxInclusive = p.taxType == com.example.retailpos.data.local.entity.TaxType.INCLUSIVE,
                currentStock = p.currentStock,
                minStock = p.minStock,
                taxType = p.taxType.name,
                updatedAt = p.updatedAt
            )
        }
    )

/** Builds the server CUSTOMER_UPSERT command from local master data. */
fun buildCustomerUpsert(installationId: String, customers: List<CustomerEntity>): CustomerUpsertPayload =
    CustomerUpsertPayload(
        customers = customers.map { c ->
            CustomerMasterPayload(
                id = c.id,
                name = c.name,
                phone = c.phone,
                currentBalancePaise = rupeesToPaiseString(c.currentBalance),
                creditLimitPaise = rupeesToPaiseString(c.creditLimit),
                updatedAt = c.updatedAt
            )
        }
    )

/** Builds the server CUSTOMER_PAYMENT command from a local Khata payment. */
fun buildCustomerPaymentCommand(
    installationId: String,
    localTransactionId: String,
    customerId: String,
    amount: Double,
    paymentMethod: com.example.retailpos.data.local.entity.PaymentMethod,
    notes: String?
): CustomerPaymentPayload =
    CustomerPaymentPayload(
        installationId = installationId,
        localTransactionId = localTransactionId,
        customerId = customerId,
        amountPaise = rupeesToPaiseString(amount),
        paymentMethod = paymentMethodToWire(paymentMethod),
        notes = notes
    )

/** Builds the server EXPENSE_PUSH command from a local expense. */
fun buildExpensePushCommand(
    installationId: String,
    localTransactionId: String,
    expense: com.example.retailpos.data.local.entity.ExpenseEntity
): ExpensePushPayload =
    ExpensePushPayload(
        installationId = installationId,
        localTransactionId = localTransactionId,
        localId = expense.localId,
        category = expense.category,
        amountPaise = rupeesToPaiseString(expense.amount),
        date = java.time.Instant.ofEpochMilli(expense.date).toString(),
        paymentMethod = expense.paymentMethod,
        notes = expense.notes.ifEmpty { null }
    )
