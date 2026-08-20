package com.retailpos.app.core.payment

import android.net.Uri

object UpiPaymentIntent {
    fun build(vpa: String, payeeName: String, amount: Double, transactionRef: String): Uri {
        require(vpa.isNotBlank()) { "Store UPI VPA is required" }
        require(amount.isFinite() && amount > 0.0) { "UPI amount must be greater than zero" }
        require(transactionRef.isNotBlank()) { "UPI transaction reference is required" }
        return Uri.parse(
            "upi://pay" +
                "?pa=${Uri.encode(vpa.trim())}" +
                "&pn=${Uri.encode(payeeName.trim())}" +
                "&am=${Uri.encode(String.format(java.util.Locale.US, "%.2f", amount))}" +
                "&cu=INR" +
                "&tr=${Uri.encode(transactionRef.trim())}"
        )
    }
}
