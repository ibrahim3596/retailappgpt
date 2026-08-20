package com.retailpos.app

import java.util.Locale

fun Double.clean(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.3f", this).trimEnd('0').trimEnd('.')
    }
