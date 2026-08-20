package com.retailpos.app.core.pos

/** Process-local favorites until store-scoped persistence is wired through the main database path. */
object FavoriteProductStore {
    private val ids = LinkedHashSet<String>()

    @Synchronized
    fun isFavorite(productId: String): Boolean = ids.contains(productId)

    @Synchronized
    fun toggle(productId: String): Boolean {
        if (productId.isBlank()) return false
        return if (ids.remove(productId)) false else {
            ids.add(productId)
            true
        }
    }

    @Synchronized
    fun all(): List<String> = ids.toList()

    @Synchronized
    fun clear() = ids.clear()
}
