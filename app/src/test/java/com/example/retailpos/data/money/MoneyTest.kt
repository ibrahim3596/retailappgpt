package com.example.retailpos.data.money

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `converts rupees to exact paise`() {
        assertEquals(12999L, Money.fromMajor("129.99").paise)
        assertEquals(125999L, Money.fromMajor("1,259.99".replace(",", "")).paise)
    }

    @Test
    fun `rounds half up to paise`() {
        assertEquals(100L, Money.fromMajor(BigDecimal("0.999")).paise)
    }

    @Test
    fun `adds and subtracts without floating point drift`() {
        val result = Money.fromMajor("10.10") + Money.fromMajor("0.20") - Money.fromMajor("0.30")
        assertEquals(1000L, result.paise)
    }

    @Test
    fun `supports exact comparisons`() {
        assertTrue(Money.fromMajor("10.01") > Money.fromMajor("10.00"))
    }
}
