package com.retailpos.app.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch as coroutineLaunch

/** Compatibility bridge for legacy screen files missing the explicit launch import. */
fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job = coroutineLaunch(block = block)
