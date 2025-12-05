package com.reflection.thecampus.utils

object ProfanityFilter {

    // Expanded list (SAFE general profanity only)
    private val baseProfanity = setOf(
        // English
        "fuck","fuk","shit","bitch","asshole","bastard","crap",
        "dick","pussy","cock","whore","slut","retard","idiot",
        "stupid","dumb","moron","imbecile","jerk","prick","loser",

        // Hindi / Hinglish
        "chutiya","chutya","chutia","chuitya",
        "bhosdike","bhosadi","bhosdi","bhonsdike","bhsdk",
        "madarchod","madrchod","madarchod",
        "behenchod","bhenchod","bhainchod",
        "gandu","gaand","ganduu",
        "randi","harami","kamina",
        "saala","sala","kutta","kutti",
        "loda","lauda","lodu","lund",
        "chut","choot","jhaatu","jhant"
    )

    private val leetMap: Map<Char, String> = mapOf(
        '@' to "a", '4' to "a",
        '0' to "o", '1' to "i", '!' to "i",
        '3' to "e", '$' to "s",

        // characters we want to REMOVE completely
        '*' to "",
        '#' to "",
        '_' to "",
        '-' to ""
    )


    // Normalize input: remove punctuation, convert leetspeak, collapse duplicates
    private fun normalize(text: String): String {
        var cleaned = text.lowercase()

        // Remove spaces and symbols (except alphanumeric and Devanagari)
        cleaned = cleaned.replace("[^a-zA-Z0-9\\u0900-\\u097F]".toRegex(), "")

        // Leetspeak mapping
        cleaned = cleaned.map { ch -> leetMap[ch] ?: ch.toString() }.joinToString("")

        // Collapse repeated characters: "chuuutiiiyaa" → "chutiya"
        cleaned = cleaned.replace("(.)\\1{2,}".toRegex(), "$1")

        return cleaned
    }

    // Generate regex patterns for robust detection
    private val regexPatterns = baseProfanity.map { word ->
        word.map { ch ->
            when (ch) {
                'a' -> "[a@4]*"
                'i' -> "[i1!]*"
                'o' -> "[o0]*"
                'u' -> "[u]*"
                'e' -> "[e3]*"
                else -> "$ch+"
            }
        }.joinToString("").toRegex(RegexOption.IGNORE_CASE)
    }

    fun containsProfanity(text: String): Boolean {
        val normalized = normalize(text)

        // Fast exact check
        if (baseProfanity.any { normalized.contains(it) }) return true

        // Regex pattern fallback
        return regexPatterns.any { it.containsMatchIn(normalized) }
    }

    fun filterProfanity(text: String): String {
        var result = text
        val normalized = normalize(text)

        baseProfanity.forEach { word ->
            if (normalized.contains(word)) {
                val stars = "*".repeat(word.length)
                result = result.replace(word, stars, ignoreCase = true)
            }
        }
        return result
    }

    fun getProfaneWords(text: String): List<String> {
        val normalized = normalize(text)
        return baseProfanity.filter { normalized.contains(it) }
    }

    fun validateMessage(text: String): String? {
        if (text.isBlank()) return "Message cannot be empty"
        val words = getProfaneWords(text)
        return if (words.isNotEmpty())
            "Message contains inappropriate language: ${words.joinToString(", ")}"
        else null
    }
}
