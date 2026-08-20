package com.retailpos.app.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.retailpos.app.core.products.VoiceLanguage
import com.retailpos.app.core.products.VoiceLanguages

@Composable
fun VoiceBillingButton(
    onTranscript: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedLanguage by remember(context) { mutableStateOf(VoiceLanguages.loadSelected(context)) }
    var listening by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var languageStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun selectedIntent(language: VoiceLanguage): Intent = createIntent(language.tag)

    fun refreshSupport(language: VoiceLanguage) {
        if (Build.VERSION.SDK_INT < 33) {
            languageStatus = languageStatus + (language.tag to "Status unavailable")
            return
        }
        val currentRecognizer = recognizer ?: return
        currentRecognizer.checkRecognitionSupport(
            selectedIntent(language),
            context.mainExecutor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(support: android.speech.RecognitionSupport) {
                    languageStatus = when {
                        VoiceLanguages.matchesLocale(support.installedOnDeviceLanguages, language.tag) -> languageStatus + (language.tag to "Downloaded")
                        VoiceLanguages.matchesLocale(support.pendingOnDeviceLanguages, language.tag) -> languageStatus + (language.tag to "Downloading")
                        VoiceLanguages.matchesLocale(support.supportedOnDeviceLanguages, language.tag) -> languageStatus + (language.tag to "Available to download")
                        VoiceLanguages.matchesLocale(support.onlineLanguages, language.tag) -> languageStatus + (language.tag to "Online")
                        else -> languageStatus + (language.tag to "Not supported")
                    }
                }

                override fun onError(error: Int) {
                    languageStatus = languageStatus + (language.tag to "Status unavailable")
                }
            }
        )
    }

    fun downloadLanguage(language: VoiceLanguage) {
        if (Build.VERSION.SDK_INT < 33) {
            onError("Language-pack downloads require Android 13 or newer. This language may still work through the device's online recognizer when supported.")
            return
        }
        val currentRecognizer = recognizer ?: run {
            onError("Voice recognition is not available on this device.")
            return
        }
        languageStatus = languageStatus + (language.tag to "Starting download")
        val intent = selectedIntent(language)
        if (Build.VERSION.SDK_INT >= 34) {
            currentRecognizer.triggerModelDownload(
                intent,
                context.mainExecutor,
                object : ModelDownloadListener {
                    override fun onProgress(completedPercent: Int) {
                        languageStatus = languageStatus + (language.tag to "Downloading $completedPercent%")
                    }
                    override fun onScheduled() {
                        languageStatus = languageStatus + (language.tag to "Download scheduled")
                    }
                    override fun onSuccess() {
                        languageStatus = languageStatus + (language.tag to "Downloaded")
                    }
                    override fun onError(error: Int) {
                        languageStatus = languageStatus + (language.tag to "Download failed")
                        onError("Could not download ${language.englishName} voice support. The device may require Wi-Fi or may not provide that speech model.")
                    }
                }
            )
        } else {
            currentRecognizer.triggerModelDownload(intent)
            languageStatus = languageStatus + (language.tag to "Download requested")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingStart) {
            pendingStart = false
            recognizer?.startListening(selectedIntent(selectedLanguage))
            listening = true
        } else if (!granted) {
            pendingStart = false
            onError("Microphone permission is required for voice billing.")
        }
    }

    DisposableEffect(context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Voice recognition is not available on this device.")
        }

        recognizer = runCatching {
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }.getOrNull()

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() { listening = true }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that. Try the selected language again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the microphone and speak your order."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "${selectedLanguage.englishName} is not supported by this recognizer. Choose another language."
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "${selectedLanguage.englishName} is supported but its speech model is unavailable. Use the language menu to download it."
                    else -> "Voice recognition failed. You can still add the item manually."
                }
                onError(message)
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                if (!text.isNullOrBlank()) onTranscript(text) else onError("No usable speech was recognized.")
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    Row(modifier = modifier) {
        IconButton(
            onClick = {
                if (listening) {
                    recognizer?.stopListening()
                    listening = false
                    return@IconButton
                }
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    recognizer?.startListening(selectedIntent(selectedLanguage))
                    listening = true
                } else {
                    pendingStart = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (listening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (listening) "Stop voice billing" else "Voice billing",
                tint = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(onClick = { languageMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Language, contentDescription = "Voice language")
        }

        DropdownMenu(expanded = languageMenuExpanded, onDismissRequest = { languageMenuExpanded = false }) {
            VoiceLanguages.SUPPORTED.forEach { language ->
                val status = languageStatus[language.tag]
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(language.nativeName)
                            Text(
                                status?.let { "${language.englishName} • $it" } ?: language.englishName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        selectedLanguage = language
                        VoiceLanguages.saveSelected(context, language)
                        refreshSupport(language)
                        languageMenuExpanded = false
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= 33) {
                DropdownMenuItem(
                    text = { Text("Download ${selectedLanguage.englishName} voice pack") },
                    onClick = {
                        downloadLanguage(selectedLanguage)
                        languageMenuExpanded = false
                    }
                )
            }
        }
    }
}

private fun createIntent(languageTag: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak product and quantity")
}
