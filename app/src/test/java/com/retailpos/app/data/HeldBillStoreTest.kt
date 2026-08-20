package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HeldBillStoreTest {
    @Before
    fun reset() {
        HeldBillStore.clear()
    }

    @Test
    fun holdAndTakePreservesDecimalLooseQuantity() {
        val id = HeldBillStore.hold(listOf(CartLine("p1", "Sugar", "SUGAR", "kg", 52.0, 0.5)))
        val held = HeldBillStore.take(id)
        assertNotNull(held)
        assertEquals(0.5, held!!.lines.single().quantity, 0.0001)
        assertNull(HeldBillStore.take(id))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyBillCannotBeHeld() {
        HeldBillStore.hold(emptyList())
    }
}
