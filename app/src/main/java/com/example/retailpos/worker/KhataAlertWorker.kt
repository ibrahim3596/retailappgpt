package com.example.retailpos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class KhataAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val storeId = inputData.getString("storeId") ?: return Result.failure()
            checkKhataAlerts(storeId)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkKhataAlerts(storeId: String) {
        val customerDao = db.customerDao()

        // Get all customers with credit balance
        val customers = customerDao.getAllCustomers(storeId).first()

        var overdueCount = 0
        var overLimitCount = 0
        var totalOutstanding = 0.0

        for (customer in customers) {
            if (customer.currentBalance > 0) {
                totalOutstanding += customer.currentBalance
                if (customer.currentBalance > customer.creditLimit) {
                    overLimitCount++
                }
                // For overdue, we'd need to track last payment date - simplified for now
                // In a real implementation, check invoice due dates from credit_ledger
            }
        }

        val prefs = applicationContext.getSharedPreferences("khata_alerts", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("${storeId}_overdue", overdueCount)
            .putInt("${storeId}_over_limit", overLimitCount)
            .putLong("${storeId}_total_outstanding", java.lang.Double.doubleToRawLongBits(totalOutstanding))
            .apply()
    }

    companion object {
        const val WORK_NAME = "khata_alert_work"
        const val TAG = "khata_alerts"

        fun schedulePeriodicCheck(context: Context, storeId: String) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<KhataAlertWorker>(1, TimeUnit.HOURS)
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