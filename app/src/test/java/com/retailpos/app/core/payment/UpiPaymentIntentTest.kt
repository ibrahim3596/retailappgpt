package com.retailpos.app.core.payment

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpiPaymentIntentTest {
    @Test
    fun `builds encoded INR UPI uri`() {
        val uri = UpiPaymentIntent.build("shop name@upi", "My Shop", 125.5, "ref-1")
        assertEquals("upi", uri.scheme)
        assertEquals("pay", uri.host)
        assertTrue(uri.getQueryParameter("pa").orEmpty().contains("shop name@upi"))
        assertEquals("My Shop", uri.getQueryParameter("pn"))
        assertEquals("125.50", uri.getQueryParameter("am"))
        assertEquals("INR", uri.getQueryParameter("cu"))
        assertEquals("ref-1", uri.getQueryParameter("tr"))
    }
}
