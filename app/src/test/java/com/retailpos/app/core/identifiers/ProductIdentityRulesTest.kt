package com.retailpos.app.core.identifiers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductIdentityRulesTest {
    @Test
    fun normalizeSku_trimsAndUppercases() {
        assertTrue(ProductIdentityRules.normalizeSku("  ab-123  ") == "AB-123")
    }

    @Test
    fun skuRejectsWhitespaceAndOversizedValues() {
        assertFalse(ProductIdentityRules.isValidSku("AB 123"))
        assertFalse(ProductIdentityRules.isValidSku("A".repeat(65)))
        assertTrue(ProductIdentityRules.isValidSku("AB-123"))
    }

    @Test
    fun productNameRequiresMeaningfulLength() {
        assertFalse(ProductIdentityRules.isValidProductName(""))
        assertTrue(ProductIdentityRules.isValidProductName("Milk 500 ml"))
    }

    @Test
    fun identifierConflictUsesNormalizedValues() {
        assertTrue(ProductIdentityRules.identifiersConflict(" 8901234567890 ", "8901234567890"))
        assertFalse(ProductIdentityRules.identifiersConflict("8901234567890", "8901234567891"))
    }
}
