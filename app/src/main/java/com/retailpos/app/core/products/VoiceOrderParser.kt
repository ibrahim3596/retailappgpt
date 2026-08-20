package com.retailpos.app.core.products

/** Splits a spoken retail order into independently validated quantity/product commands. */
object VoiceOrderParser {
    private val CONNECTORS = Regex(
        "\\s+(?:aur|and|plus|और|आणि|మరియు|ഒപ്പം|കൂടാതെ|மற்றும்|ಮತ್ತು|এবং|અને|ਅਤੇ|ଏବଂ)\\s+|\\s*[,;]+\\s*",
        RegexOption.IGNORE_CASE
    )

    fun parse(spoken: String): List<VoiceSaleCommand>? {
        val text = spoken.trim()
        if (text.isBlank()) return null

        val parts = CONNECTORS.split(text).map { it.trim() }.filter(String::isNotBlank)
        if (parts.isEmpty()) return null

        val commands = parts.map { VoiceSaleCommandParser.parse(it) ?: return null }
        return commands
    }
}
