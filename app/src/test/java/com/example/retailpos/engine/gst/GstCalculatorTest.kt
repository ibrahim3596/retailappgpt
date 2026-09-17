package com.example.retailpos.engine.gst

import org.junit.Assert.assertEquals
import org.junit.Test

class GstCalculatorTest {

    @Test
    fun `tax inclusive 18% gst splits correctly`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 90.0,
            quantity = 1.0,
            gstRate = 18.0,
            isTaxInclusive = true
        )
        // 90 / 1.18 = 76.27, tax = 90 - 76.27 = 13.73
        assertEquals(76.27, result.assessableValue, 0.01)
        assertEquals(13.73, result.totalGst, 0.01)
        assertEquals(6.87, result.cgstAmount, 0.01)
        assertEquals(6.86, result.sgstAmount, 0.01)
        assertEquals(0.0, result.igstAmount, 0.01)
        assertEquals(90.0, result.finalLineTotal, 0.01)
    }

    @Test
    fun `tax exclusive 18% gst adds tax on top`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 100.0,
            quantity = 1.0,
            gstRate = 18.0,
            isTaxInclusive = false
        )
        assertEquals(100.0, result.assessableValue, 0.01)
        assertEquals(18.0, result.totalGst, 0.01)
        assertEquals(9.0, result.cgstAmount, 0.01)
        assertEquals(9.0, result.sgstAmount, 0.01)
        assertEquals(0.0, result.igstAmount, 0.01)
        assertEquals(118.0, result.finalLineTotal, 0.01)
    }

    @Test
    fun `interstate sale uses igst instead of cgst sgst`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 100.0,
            quantity = 1.0,
            gstRate = 18.0,
            isTaxInclusive = false,
            isInterstate = true
        )
        assertEquals(0.0, result.cgstAmount, 0.01)
        assertEquals(0.0, result.sgstAmount, 0.01)
        assertEquals(18.0, result.igstAmount, 0.01)
        assertEquals(118.0, result.finalLineTotal, 0.01)
    }

    @Test
    fun `zero gst rate means no tax`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 50.0,
            quantity = 2.0,
            gstRate = 0.0,
            isTaxInclusive = true
        )
        assertEquals(100.0, result.assessableValue, 0.01)
        assertEquals(0.0, result.totalGst, 0.01)
        assertEquals(100.0, result.finalLineTotal, 0.01)
    }

    @Test
    fun `quantity multiplication is correct`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 33.33,
            quantity = 3.0,
            gstRate = 5.0,
            isTaxInclusive = false
        )
        val grossPrice = 33.33 * 3
        assertEquals(grossPrice, result.grossPrice, 0.01)
        assertEquals(grossPrice, result.assessableValue, 0.01)
        val expectedTax = grossPrice * 0.05
        assertEquals(expectedTax, result.totalGst, 0.01)
        assertEquals(grossPrice + expectedTax, result.finalLineTotal, 0.01)
    }

    @Test
    fun `discount reduces assessable value for tax inclusive`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 100.0,
            quantity = 1.0,
            gstRate = 18.0,
            isTaxInclusive = true,
            discountAmount = 10.0
        )
        val discountedGross = 90.0
        val assessable = 90.0 / 1.18
        assertEquals(assessable, result.assessableValue, 0.01)
        assertEquals(discountedGross - assessable, result.totalGst, 0.01)
        assertEquals(discountedGross, result.finalLineTotal, 0.01)
    }

    @Test
    fun `discount does not reduce below zero`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 50.0,
            quantity = 1.0,
            gstRate = 18.0,
            isTaxInclusive = true,
            discountAmount = 60.0
        )
        // discountedGross = max(50 - 60, 0) = 0
        assertEquals(0.0, result.assessableValue, 0.01)
        assertEquals(0.0, result.totalGst, 0.01)
        assertEquals(0.0, result.finalLineTotal, 0.01)
    }

    @Test
    fun `odd gst split cgst plus sgst equals total gst`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 100.0,
            quantity = 1.0,
            gstRate = 12.0,
            isTaxInclusive = false
        )
        // totalGst = 12.0, odd split: cgst=6.0, sgst=6.0
        assertEquals(result.cgstAmount + result.sgstAmount, result.totalGst, 0.01)
    }

    @Test
    fun `odd total gst preserves exact sum`() {
        val result = GstCalculator.calculateItemGst(
            sellingPrice = 100.0,
            quantity = 1.0,
            gstRate = 5.0,
            isTaxInclusive = false
        )
        // totalGst = 5.0, cgst=2.5, sgst=2.5
        assertEquals(result.cgstAmount + result.sgstAmount, result.totalGst, 0.01)
    }
}
