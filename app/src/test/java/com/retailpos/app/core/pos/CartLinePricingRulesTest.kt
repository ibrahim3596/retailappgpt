package com.retailpos.app.core.pos

import com.retailpos.app.core.staff.StaffRole
import com.retailpos.app.data.CartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CartLinePricingRulesTest {
    private val line = CartLine("p1", "Sugar", "S1", "kg", 60.0, 1.0)

    @Test
    fun cashierCannotApplyItemDiscount() {
        val updated = line.copy(itemDiscountAmount = 5.0)
        assertEquals(
            "This staff role cannot apply item discounts.",
            CartLinePricingRules.validate(updated, StaffRole.CASHIER)
        )
    }

    @Test
    fun managerCanApplyItemDiscountWithinLineValue() {
        val updated = line.copy(itemDiscountAmount = 5.0)
        assertNull(CartLinePricingRules.validate(updated, StaffRole.MANAGER))
        assertEquals(55.0, updated.lineTotal, 0.0)
    }

    @Test
    fun cashierCannotOverridePrice() {
        val updated = line.copy(overrideUnitPrice = 55.0)
        assertEquals(
            "This staff role cannot override selling prices.",
            CartLinePricingRules.validate(updated, StaffRole.CASHIER)
        )
    }

    @Test
    fun managerCanOverridePriceAndDiscount() {
        val updated = line.copy(overrideUnitPrice = 55.0, itemDiscountAmount = 5.0)
        assertNull(CartLinePricingRules.validate(updated, StaffRole.MANAGER))
        assertEquals(50.0, updated.lineTotal, 0.0)
    }

    @Test
    fun discountCannotExceedGrossLineValue() {
        val updated = line.copy(itemDiscountAmount = 60.01)
        assertEquals(
            "Item discount cannot exceed the line value.",
            CartLinePricingRules.validate(updated, StaffRole.MANAGER)
        )
    }
}
