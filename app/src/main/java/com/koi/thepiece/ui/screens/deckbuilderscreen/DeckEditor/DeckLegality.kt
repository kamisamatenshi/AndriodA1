package com.koi.thepiece.ui.screens.deckbuilderscreen

import com.koi.thepiece.data.model.Card

sealed class DeckIssue(val message: String) {
    class Error(msg: String) : DeckIssue(msg)
    class Warning(msg: String) : DeckIssue(msg)
}

data class DeckLegalityResult(
    val isLegal: Boolean,
    val errors: List<DeckIssue.Error>,
    val warnings: List<DeckIssue.Warning>
)

object DeckLegality {
    private val forbiddenCardNumbers: Set<String> = setOf(
        "OP02-117","OP06-116","OP03-098","OP06-086","EB01-059",
        "OP07-045","OP03-040","ST06-015","ST10-001","OP02-024"
    )

    fun check(
        leader: Card?,
        deckMap: Map<Int, Int>,   // cardId -> qty
        allCards: List<Card>,
        requireExactly50: Boolean = false
    ): DeckLegalityResult {

        val errors = mutableListOf<DeckIssue.Error>()
        val warnings = mutableListOf<DeckIssue.Warning>()

        if (leader == null) {
            errors += DeckIssue.Error("No Leader selected (Leader must be 1 card).")
            return DeckLegalityResult(false, errors, warnings)
        }

        val deckCards: List<Pair<Card, Int>> = deckMap.mapNotNull { (cardId, qty) ->
            val c = allCards.firstOrNull { it.id == cardId }
            if (c == null) null else (c to qty)
        }

        val totalMain = deckCards.sumOf { it.second }
        if (requireExactly50) {
            if (totalMain != 50) errors += DeckIssue.Error("Main deck must be exactly 50 cards. (Now: $totalMain)")
        } else {
            if (totalMain > 50) errors += DeckIssue.Error("Main deck cannot exceed 50 cards. (Now: $totalMain)")
        }

        // Leader color rule
        val leaderColors = parseColors(leader.color)

        // Collect deck colors used (ignore "mix" if it ever appears on cards)
        val deckColorsUsed = deckCards
            .flatMap { (card, _) -> parseColors(card.color).toList() }
            .filter { it != "mix" }
            .toSet()

        if (leaderColors.contains("mix")) {
            // Mix leader: deck can use at most 2 colors total
            if (deckColorsUsed.size > 2) {
                errors += DeckIssue.Error("Mix leader allows at most 2 colors, but deck uses: ${deckColorsUsed.joinToString()}.")
            }
        } else {
            // Normal leader: deck colors must be subset of leader colors
            if (!deckColorsUsed.all { it in leaderColors }) {
                errors += DeckIssue.Error("Deck contains colors not included in Leader (${leader.color}).")
            }
        }

        // Max 4 copies per card number (same card code/number)
        // Your Card.code should be like "OP01-xxx". If your code includes variant text, normalize it.
        val copiesByNumber = mutableMapOf<String, Int>()
        deckCards.forEach { (card, qty) ->
            val num = (card.code ?: "").trim()
            if (num.isNotEmpty()) copiesByNumber[num] = (copiesByNumber[num] ?: 0) + qty
        }
        copiesByNumber.forEach { (num, count) ->
            if (count > 4) {
                errors += DeckIssue.Error("Too many copies: $num has $count copies (max 4).")
            }
        }

        // Forbidden list check
        copiesByNumber.keys.forEach { num ->
            if (num in forbiddenCardNumbers) {
                errors += DeckIssue.Error("Forbidden card in deck: $num")
            }
        }

        // Optional: warn if deck not complete
        if (!requireExactly50 && totalMain < 50) {
            warnings += DeckIssue.Warning("Deck incomplete: $totalMain/50")
        }

        val legal = errors.isEmpty()
        return DeckLegalityResult(legal, errors, warnings)
    }

    private fun parseColors(raw: String?): Set<String> {
        // Examples you might have: "Red", "Red/Green", "RED", "Blue & Purple"
        return (raw ?: "")
            .split("/", "&", ",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}