package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize as composeFillMaxSize
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import kotlin.reflect.KProperty

/** Compatibility operators for older Compose call sites that omit extension imports. */
operator fun <T> State<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

fun Modifier.fillMaxSize(): Modifier = composeFillMaxSize()
