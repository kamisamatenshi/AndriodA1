package com.koi.thepiece.ui.screens.catalogscreen

import android.util.Log
import com.koi.thepiece.ui.screens.catalogscreen.components.OpJpMaps

object CatalogSearchQueryExpander {
    /** Normalizes English/romaji-ish input so it matches your map keys. */
    fun normalizeLatin(input: String): String {
        return input
            .lowercase()
            .trim()
            .replace(Regex("[’`´]"), "'")
            .replace(".", "")
            .replace("・", " ")
            .replace(Regex("[^a-z0-9\\s'\\-]"), " ") // keep basic latin + digits
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** If the query contains any Japanese chars, skip latin-normalization logic. */
    fun containsJapanese(input: String): Boolean {
        // Hiragana \u3040-\u309F, Katakana \u30A0-\u30FF, Kanji \u4E00-\u9FFF
        return input.any { ch ->
            ch in '\u3040'..'\u309F' || ch in '\u30A0'..'\u30FF' || ch in '\u4E00'..'\u9FFF'
        }
    }

    /**
     * Returns a set of search tokens:
     * - original query (lowercased)
     * - mapped JP name/trait strings (if any)
     * - also include the normalized key itself (helps when cards store romanized too)
     */
    fun expand(rawQuery: String): Set<String> {
        val rawTrim = rawQuery.trim()                   // Removes leading/trailing spaces.
        if (rawTrim.isEmpty()) return emptySet()        // If the user types nothing, return no search tokens.

        val tokens = linkedSetOf(rawTrim.lowercase())   // Changes the original input to lowercase

        if (containsJapanese(rawTrim)) {
            tokens += rawTrim
            return tokens
        }

        val key = normalizeLatin(rawTrim)       // Removes punctuation/ standardizes spaces/ removes accents
        if (key.isEmpty()) return tokens

        tokens += key

        OpJpMaps.NAME_MAP[key]?.let { tokens+= it }
        OpJpMaps.TRAITS_MAP[key]?.let { tokens += it }

        return tokens
    }
}