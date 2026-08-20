package com.retailpos.app.core.pos

/** Keeps the first-seen product order from recent sale lines. */
object RecentProductRules {
    fun fromSaleLines(
        saleLinesNewestFirst: List<List<QuickAddProduct>>,
        limit: Int = 12
    ): List<QuickAddProduct> = QuickAddRules.dedupeInOrder(saleLinesNewestFirst.flatten(), limit)
}
