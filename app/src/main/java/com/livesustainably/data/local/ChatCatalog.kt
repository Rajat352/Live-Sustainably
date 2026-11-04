package com.livesustainably.data.local

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal enum class ChatReplyKind {
    Text,
    Week,
    Month
}

@Serializable
internal data class ChatIntent(
    val id: String,
    val kind: ChatReplyKind,
    val phrases: List<String>,
    val reply: String,
)

@Serializable
internal data class ChatCatalog(
    val fallback: String,
    val intents: List<ChatIntent>
) {
    fun match(message: String): ChatIntent? {
        val input = " ${normalize(message)} "
        return intents.firstOrNull { intent ->
            intent.phrases.any { " ${normalize(it)} " in input }
        }
    }

    companion object {
        fun parse(source: String): ChatCatalog = Json.decodeFromString<ChatCatalog>(source).also { catalog ->
            require(catalog.fallback.isNotBlank() && catalog.fallback.length <= 2000)
            require(catalog.intents.isNotEmpty() && catalog.intents.size <= 100)
            require(catalog.intents.distinctBy { it.id }.size == catalog.intents.size)
            catalog.intents.forEach {
                require(it.id.isNotBlank() && it.phrases.isNotEmpty())
                require(it.phrases.all { phrase -> normalize(phrase).isNotBlank() })
                require(it.reply.length <= 2000 && (it.kind != ChatReplyKind.Text || it.reply.isNotBlank()))
            }
        }

        private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .lowercase(Locale.ROOT).replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    }
}
