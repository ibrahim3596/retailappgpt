package com.retailpos.app.core.payment

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingPaymentStoreConcurrencyTest {
    @Test
    fun concurrentRequestsForSameFingerprintReuseOneCreatedKey() {
        PendingPaymentStore.clear()
        val createCount = AtomicInteger(0)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val keys = arrayOfNulls<String>(2)

        repeat(2) { index ->
            Thread {
                start.await()
                keys[index] = PendingPaymentStore.getOrCreateIdempotencyKey("fingerprint") {
                    createCount.incrementAndGet()
                    "checkout-key"
                }
                done.countDown()
            }.start()
        }

        start.countDown()
        done.await()

        assertEquals(1, createCount.get())
        assertEquals(keys[0], keys[1])
        PendingPaymentStore.clear()
    }
}
