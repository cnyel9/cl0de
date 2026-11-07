package com.cl0de.app.data

import com.cl0de.app.Graph
import java.util.Locale

/**
 * App language: "id" (Bahasa Indonesia, the default) or "en" (English).
 * It drives three things: the reply-language rule kept in the global AGENTS.md,
 * the default language for dictation and read-aloud, and the language of the
 * app's own short messages where a choice matters.
 */
object AppLang {
    val options = listOf("id" to "Bahasa Indonesia", "en" to "English")

    fun current(): String = Graph.prefs.appLang
    fun indonesian(): Boolean = current() != "en"
    fun label(): String = options.firstOrNull { it.first == current() }?.second ?: "Bahasa Indonesia"

    /** Pick one of two short strings in the app language. */
    fun s(id: String, en: String): String = if (indonesian()) id else en

    /** BCP-47 tag for speech recognition and read-aloud when no speech language was picked explicitly. */
    fun speechTag(): String = if (indonesian()) "id-ID" else "en-US"
    fun locale(): Locale = Locale.forLanguageTag(speechTag())

    /** The managed AGENTS.md line that makes the agent answer in the app language. */
    fun ruleLine(): String = s("Selalu jawab dalam Bahasa Indonesia.", "Always reply in English.")

    /** Write the rule into the global AGENTS.md. It lives in its own marked block inside the
     * user rules, so the user's other rules are never touched; unchanged when already current. */
    fun applyRule() = Memory.setLanguageRule(ruleLine())
}
