package com.example.retailpos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(applicationContext)
    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        return try {
            val storeId = inputData.getString("storeId") ?: return Result.failure()
            val alertType = inputData.getString("alertType") ?: return Result.failure()

            createNotificationChannel()
            checkAndNotify(storeId, alertType)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "inventory_alerts",
                "Inventory Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Low stock, out of stock, and expiry alerts"
            }
            val syncChannel = NotificationChannel(
                "sync_alerts",
                "Sync Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pending sync, conflicts, and failed sync alerts"
            }
            val khataChannel = NotificationChannel(
                "khata_alerts",
                "Khata/Credit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Overdue and over-limit customer credit alerts"
            }
            notificationManager.createNotificationChannels(
                listOf(channel, syncChannel, khataChannel)
            )
        }
    }

    private suspend fun checkAndNotify(storeId: String, alertType: String) {
        when (alertType) {
            "inventory" -> notifyInventoryAlerts(storeId)
            "sync" -> notifySyncAlerts(storeId)
            "khata" -> notifyKhataAlerts(storeId)
        }
    }

    private suspend fun notifyInventoryAlerts(storeId: String) {
        val prefs = applicationContext.getSharedPreferences("inventory_alerts", Context.MODE_PRIVATE)
        val lowStock = prefs.getInt("${storeId}_low_stock", 0)
        val outOfStock = prefs.getInt("${storeId}_out_of_stock", 0)
        val nearExpiry = prefs.getInt("${storeId}_near_expiry", 0)
        val expired = prefs.getInt("${storeId}_expired", 0)

        val totalAlerts = lowStock + outOfStock + nearExpiry + expired
        if (totalAlerts == 0) return

        val title = "Inventory Alert"
        val body = buildInventoryMessage(lowStock, outOfStock, nearExpiry, expired)

        val notification = NotificationCompat.Builder(applicationContext, "inventory_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private suspend fun notifySyncAlerts(storeId: String) {
        val prefs = applicationContext.getSharedPreferences("sync_alerts", Context.MODE_PRIVATE)
        val pending = prefs.getInt("${storeId}_pending_sync", 0)
        val conflicts = prefs.getInt("${storeId}_conflicts", 0)
        val failed = prefs.getInt("${storeId}_failed_sync", 0)

        val totalAlerts = pending + conflicts + failed
        if (totalAlerts == 0) return

        val title = "Sync Alert"
        val body = buildSyncMessage(pending, conflicts, failed)

        val notification = NotificationCompat.Builder(applicationContext, "sync_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }

    private suspend fun notifyKhataAlerts(storeId: String) {
        val prefs = applicationContext.getSharedPreferences("khata_alerts", Context.MODE_PRIVATE)
        val overdue = prefs.getInt("${storeId}_overdue", 0)
        val overLimit = prefs.getInt("${storeId}_over_limit", 0)
        val totalOutstandingBits = prefs.getLong("${storeId}_total_outstanding", 0)
        val totalOutstanding = java.lang.Double.longBitsToDouble(totalOutstandingBits)

        val totalAlerts = overdue + overLimit
        if (totalAlerts == 0) return

        val title = "Credit Alert"
        val body = "Customers overdue: $overdue, Over limit: $overLimit. Total outstanding: ₹${String.format("%,.2f", totalOutstanding)}"

        val notification = NotificationCompat.Builder(applicationContext, "khata_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1003, notification)
    }

    private fun buildInventoryMessage(lowStock: Int, outOfStock: Int, nearExpiry: Int, expired: Int): String {
        val parts = mutableListOf<String>()
        if (outOfStock > 0) parts.add("$outOfStock out of stock")
        if (lowStock > 0) parts.add("$lowStock low stock")
        if (expired > 0) parts.add("$expired expired")
        if (nearExpiry > 0) parts.add("$nearExpiry near expiry")
        return parts.joinToString(", ")
    }

    private fun buildSyncMessage(pending: Int, conflicts: Int, failed: Int): String {
        val parts = mutableListOf<String>()
        if (pending > 0) parts.add("$pending pending")
        if (conflicts > 0) parts.add("$conflicts conflicts")
        if (failed > 0) parts.add("$failed failed")
        return parts.joinToString(", ")
    }

    companion object {
        fun scheduleNotificationCheck(context: Context, storeId: String) {
            // Schedule a one-time work that runs immediately to check and notify
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(androidx.work.Data.Builder()
                    .putString("storeId", storeId)
                    .putString("alertType", "inventory")
                    .build())
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()

            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "notification_inventory_${storeId}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // Also schedule sync and khata notifications
            val syncWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(androidx.work.Data.Builder()
                    .putString("storeId", storeId)
                    .putString("alertType", "sync")
                    .build())
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(
                "notification_sync_${storeId}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncWork
            )

            val khataWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(androidx.work.Data.Builder()
                    .putString("storeId", storeId)
                    .putString("alertType", "khata")
                    .build())
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(
                "notification_khata_${storeId}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                khataWork
            )
        }

        fun cancelNotifications(context: Context, storeId: String) {
            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.cancelUniqueWork("notification_inventory_${storeId}")
            workManager.cancelUniqueWork("notification_sync_${storeId}")
            workManager.cancelUniqueWork("notification_khata_${storeId}")
        }
    }
}