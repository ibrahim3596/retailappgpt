package com.example.retailpos.ui

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Dashboard")
    object POS : Screen("pos", "Point of Sale")
    object Products : Screen("products", "Product Catalog")
    object ProductReview : Screen("product_review/{barcode}", "Product Verification") {
        fun createRoute(barcode: String) = "product_review/$barcode"
    }
    object Inventory : Screen("inventory", "Batches & Stock")
    object Customers : Screen("customers", "Khata Ledger")
    object SyncConflicts : Screen("sync_conflicts", "Sync & Conflicts")
    object CameraScanner : Screen("camera_scanner/{mode}", "Camera & Barcode Vision") {
        fun createRoute(mode: String) = "camera_scanner/$mode"
    }
    object ReceiptPreview : Screen("receipt_preview/{invoiceId}", "Receipt Preview") {
        fun createRoute(invoiceId: String) = "receipt_preview/$invoiceId"
    }
    object Analytics : Screen("analytics", "GST & Sales Report")
    object Settings : Screen("settings", "Store Settings")
    object Setup : Screen("setup", "Store Setup")
    object Login : Screen("login", "Local Login")
}
