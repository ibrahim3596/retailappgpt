package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCaptureConsensusTest {
    @Test
    fun repeatedFramesProduceConsensusAndFrameCount() {
        val result = ProductCaptureConsensus.merge(
            listOf(
                ProductCaptureObservation(barcode = "8901234567890", printedName = "Sugar", pack = ParsedPack(500.0, "g", "500 g")),
                ProductCaptureObservation(barcode = "8901234567890", printedName = "Sugar", pack = ParsedPack(500.0, "g", "500 g")),
                ProductCaptureObservation(barcode = "8901234567890", printedName = "Sugar", pack = ParsedPack(1.0, "kg", "1 kg"))
            )
        )

        assertEquals("8901234567890", result?.barcode)
        assertEquals("Sugar", result?.printedName)
        assertEquals(500.0, result?.pack?.size ?: -1.0, 0.0)
        assertEquals(3, result?.frameCount)
    }

    @Test
    fun missingFieldsRemainMissingInsteadOfBeingInvented() {
        val result = ProductCaptureConsensus.merge(
            listOf(ProductCaptureObservation(categoryHint = "food"))
        )

        assertTrue(result?.barcode == null)
        assertTrue(result?.printedName == null)
        assertEquals("food", result?.categoryHint)
    }
}
