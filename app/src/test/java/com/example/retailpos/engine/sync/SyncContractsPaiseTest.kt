package com.example.retailpos.engine.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SyncContractsPaiseTest {

    @Test
    fun `rupeesToPaiseString converts correctly`() {
        assertEquals("10000", rupeesToPaiseString(100.0))
        assertEquals("500", rupeesToPaiseString(5.0))
        assertEquals("100", rupeesToPaiseString(1.0))
        assertEquals("0", rupeesToPaiseString(0.0))
    }

    @Test
    fun `rupeesToPaiseString handles paise precision`() {
        assertEquals("123", rupeesToPaiseString(1.23))
        assertEquals("999", rupeesToPaiseString(9.99))
    }

    @Test
    fun `paiseStringToRupees converts correctly`() {
        assertEquals(100.0, paiseStringToRupees("10000"), 0.01)
        assertEquals(5.0, paiseStringToRupees("500"), 0.01)
        assertEquals(1.0, paiseStringToRupees("100"), 0.01)
        assertEquals(0.0, paiseStringToRupees("0"), 0.01)
    }

    @Test
    fun `paiseStringToRupees handles null`() {
        assertEquals(0.0, paiseStringToRupees(null), 0.01)
    }

    @Test
    fun `paymentMethodToWire maps CASH correctly`() {
        assertEquals("CASH", paymentMethodToWire(com.example.retailpos.data.local.entity.PaymentMethod.CASH))
    }

    @Test
    fun `paymentMethodToWire maps UPI correctly`() {
        assertEquals("UPI", paymentMethodToWire(com.example.retailpos.data.local.entity.PaymentMethod.UPI))
    }

    @Test
    fun `paymentMethodToWire maps CREDIT correctly`() {
        assertEquals("CREDIT", paymentMethodToWire(com.example.retailpos.data.local.entity.PaymentMethod.CREDIT))
    }

    @Test
    fun `paymentMethodToWire maps CARD correctly`() {
        assertEquals("CARD", paymentMethodToWire(com.example.retailpos.data.local.entity.PaymentMethod.CARD))
    }

    @Test
    fun `customerPaymentCommand builds valid payload`() {
        val command = buildCustomerPaymentCommand(
            installationId = "inst-001",
            localTransactionId = "pay-001",
            customerId = "cust-001",
            amount = 500.0,
            paymentMethod = com.example.retailpos.data.local.entity.PaymentMethod.CASH,
            notes = "Khata payment"
        )
        assertNotNull(command)
        assertEquals("inst-001", command.installationId)
        assertEquals("cust-001", command.customerId)
        assertEquals("50000", command.amountPaise)
        assertEquals("CASH", command.paymentMethod)
        assertEquals("Khata payment", command.notes)
    }
}
