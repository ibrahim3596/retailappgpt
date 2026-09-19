package com.example.retailpos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class InventoryAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val storeId = inputData.getString("storeId") ?: return Result.failure()
            checkInventoryAlerts(storeId)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkInventoryAlerts(storeId: String) {
        val productDao = db.productDao()
        val batchDao = db.batchDao()

        // Get all products for this store
        val products = productDao.getAllProducts(storeId).first()

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val nearExpiryThreshold = now + TimeUnit.DAYS.toMillis(30) // 30 days

        var lowStockCount = 0
        var outOfStockCount = 0
        var nearExpiryCount = 0
        var expiredCount = 0

        for (product in products) {
            // Check low stock / out of stock
            if (product.currentStock <= product.minStock && product.currentStock > 0) {
                lowStockCount++
            } else if (product.currentStock <= 0) {
                outOfStockCount++
            }

            // Check batch expiry
            val batches = batchDao.getBatchesForProductFlow(storeId, product.id).first()
            for (batch in batches) {
                if (batch.remainingQty > 0) {
                    if (batch.expiryDate <= now) {
                        expiredCount++
                    } else if (batch.expiryDate <= nearExpiryThreshold) {
                        nearExpiryCount++
                    }
                }
            }
        }

        // Store results for notification display
        // In a real app, this would trigger actual notifications via NotificationManager
        // For now, we just log the counts - a UI observer can read these
        val prefs = applicationContext.getSharedPreferences("inventory_alerts", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("${storeId}_low_stock", lowStockCount)
            .putInt("${storeId}_out_of_stock", outOfStockCount)
            .putInt("${storeId}_near_expiry", nearExpiryCount)
            .putInt("${storeId}_expired", expiredCount)
            .putLong("${storeId}_last_check", now)
            .apply()
    }

    companion object {
        const val WORK_NAME = "inventory_alert_work"
        const val TAG = "inventory_alerts"

        fun schedulePeriodicCheck(context: Context, storeId: String) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<InventoryAlertWorker>(4, TimeUnit.HOURS)
                .setInputData(androidx.work.Data.Builder().putString("storeId", storeId).build())
                .addTag(TAG)
                .build()

            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodicCheck(context: Context) {
            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}