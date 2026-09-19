package com.example.retailpos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.SyncStatus

class SyncAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val storeId = inputData.getString("storeId") ?: return Result.failure()
            checkPendingSync(storeId)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkPendingSync(storeId: String) {
        val syncDao = db.syncDao()
        val pendingCount = syncDao.getPendingSyncCount(storeId)
        val conflictCount = syncDao.getUnresolvedConflictsCount(storeId)
        val failedCount = syncDao.getSyncLogCountByStatus(storeId, "FAILED")

        val prefs = applicationContext.getSharedPreferences("sync_alerts", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("${storeId}_pending_sync", pendingCount)
            .putInt("${storeId}_conflicts", conflictCount)
            .putInt("${storeId}_failed_sync", failedCount)
            .apply()
    }

    companion object {
        const val WORK_NAME = "sync_alert_work"
        const val TAG = "sync_alerts"

        fun schedulePeriodicCheck(context: Context, storeId: String) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<SyncAlertWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
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