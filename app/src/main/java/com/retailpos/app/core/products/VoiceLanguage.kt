package com.retailpos.app.core.products

import android.content.Context
import java.util.Locale

data class VoiceLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String
)

object VoiceLanguages {
    val DEFAULT = VoiceLanguage("hi-IN", "हिन्दी", "Hindi")

    val SUPPORTED = listOf(
        DEFAULT,
        VoiceLanguage("en-IN", "English", "English (India)"),
        VoiceLanguage("te-IN", "తెలుగు", "Telugu"),
        VoiceLanguage("ml-IN", "മലയാളം", "Malayalam"),
        VoiceLanguage("mr-IN", "मराठी", "Marathi"),
        VoiceLanguage("ta-IN", "தமிழ்", "Tamil"),
        VoiceLanguage("kn-IN", "ಕನ್ನಡ", "Kannada"),
        VoiceLanguage("bn-IN", "বাংলা", "Bengali"),
        VoiceLanguage("gu-IN", "ગુજરાતી", "Gujarati"),
        VoiceLanguage("pa-IN", "ਪੰਜਾਬੀ", "Punjabi"),
        VoiceLanguage("or-IN", "ଓଡ଼ିଆ", "Odia")
    )

    private const val PREFS = "voice_billing"
    private const val SELECTED_LANGUAGE = "selected_language"

    fun loadSelected(context: Context): VoiceLanguage {
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE, DEFAULT.tag)
            ?: DEFAULT.tag
        return SUPPORTED.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: DEFAULT
    }

    fun saveSelected(context: Context, language: VoiceLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE, language.tag)
            .apply()
    }

    fun matchesLocale(languageList: List<String>, requested: String): Boolean {
        val requestedLanguage = Locale.forLanguageTag(requested).language
        return languageList.any { candidate ->
            candidate.equals(requested, ignoreCase = true) ||
                Locale.forLanguageTag(candidate).language.equals(requestedLanguage, ignoreCase = true)
        }
    }
}
