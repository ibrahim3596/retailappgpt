package com.retailpos.app

import android.app.Application
import com.retailpos.app.core.payment.PendingPaymentStore

class RetailPosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PendingPaymentStore.configure(this)
    }
}
