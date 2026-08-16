package com.example.retailpos.util

import java.util.Locale

object PaiseUtils {

    fun rupeesToPaise(rupees: Double): Long {
        return Math.round(rupees * 100.0)
    }

    fun paiseToRupees(paise: Long): Double {
        return paise / 100.0
    }

    fun formatRupees(rupees: Double): String {
        return String.format(Locale.getDefault(), "₹%.2f", rupees)
    }

    fun formatPaise(paise: Long): String {
        return String.format(Locale.getDefault(), "₹%.2f", paise / 100.0)
    }
}

fun Double.toPaise(): Long = PaiseUtils.rupeesToPaise(this)
fun Long.toRupees(): Double = PaiseUtils.paiseToRupees(this)
fun Double.formatCurrency(): String = PaiseUtils.formatRupees(this)
fun Long.formatPaiseCurrency(): String = PaiseUtils.formatPaise(this)
