package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor

import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.QtyClass

/**
 * Represents an issue found during deck legality validation.
 *
 * DeckIssue is split into two severities:
 * - Error: makes the deck illegal (cannot be saved/validated as legal)
 * - Warning: deck is still legal but has a potential problem (e.g., incomplete)
 */
sealed class DeckIssue(val message: String) {

    /** Illegal condition that fails validation. */
    class Error(msg: String) : DeckIssue(msg)

    /** Non-fatal issue that should be surfaced to the user. */
    class Warning(msg: String) : DeckIssue(msg)
}

/**
 * Result object returned by [DeckLegality.check].
 *
 * @property isLegal True when there are no errors.
 * @property errors List of illegal conditions found during validation.
 * @property warnings List of non-fatal conditions found during validation.
 */
data class DeckLegalityResult(
    val isLegal: Boolean,
    val errors: List<DeckIssue.Error>,
    val warnings: List<DeckIssue.Warning>
)

/**
 * Deck legality validator for the deck builder.
 *
 * This object enforces core deck construction rules such as:
 * - Leader requirement
 * - Main deck size constraint (<= 50 or exactly 50)
 * - Leader color restriction rules (including "mix" leader special-case)
 * - Duplicate copy limit by card number/code (max 4 per card code)
 * - Forbidden card list (banlist)
 *
 * The validation output is returned as [DeckLegalityResult] and can be
 * displayed directly in UI (errors/warnings).
 */
object DeckLegality {

    /**
     * Forbidden card codes that are not allowed in the deck.
     * Codes are expected to match Card.code values (e.g. "OP02-117").
     */
    private val forbiddenCardNumbers: Set<String> = setOf(
        "OP02-117", "OP06-116", "OP03-098", "OP06-086", "EB01-059",
        "OP07-045", "OP03-040", "ST06-015", "ST10-001", "OP02-024"
    )

    /**
     * Validates a deck against construction rules and returns detailed errors/warnings.
     *
     * @param leader Selected leader card. Must be non-null.
     * @param deckMap Deck composition map keyed by cardId, holding required quantities.
     * @param allCards Full catalogue list used to resolve cardId -> Card data.
     * @param requireExactly50 When true: main deck must be exactly 50 cards.
     *                         When false: main deck may be <= 50 cards (warns if < 50).
     *
     * @return [DeckLegalityResult] containing legality status and issue lists.
     */
    fun check(
        leader: Card?,
        deckMap: Map<Int, QtyClass>,   // cardId -> qty
        allCards: List<Card>,
        requireExactly50: Boolean = false
    ): DeckLegalityResult {

        val errors = mutableListOf<DeckIssue.Error>()
        val warnings = mutableListOf<DeckIssue.Warning>()

        // -------------------------
        // Rule: Leader must exist
        // -------------------------
        if (leader == null) {
            errors += DeckIssue.Error("No Leader selected (Leader must be 1 card).")
            return DeckLegalityResult(false, errors, warnings)
        }

        // -------------------------
        // Resolve deck entries: cardId -> (Card, requiredQty)
        // -------------------------
        val deckCards: List<Pair<Card, Int>> = deckMap.mapNotNull { (cardId, qty) ->
            val c = allCards.firstOrNull { it.id == cardId }
            if (c == null) null else (c to qty.requiredQty)
        }

        // -------------------------
        // Rule: Deck size (<= 50 or exactly 50)
        // -------------------------
        val totalMain = deckCards.sumOf { it.second }
        if (requireExactly50) {
            if (totalMain != 50) {
                errors += DeckIssue.Error("Main deck must be exactly 50 cards. (Now: $totalMain)")
            }
        } else {
            if (totalMain > 50) {
                errors += DeckIssue.Error("Main deck cannot exceed 50 cards. (Now: $totalMain)")
            }
        }

        // -------------------------
        // Rule: Leader color restriction
        // -------------------------
        val leaderColors = parseColors(leader.color)

        // Collect all colors used by deck cards; ignore "mix" if it appears on any card.
        val deckColorsUsed = deckCards
            .flatMap { (card, _) -> parseColors(card.color).toList() }
            .filter { it != "mix" }
            .toSet()

        if (leaderColors.contains("mix")) {
            // Mix leader: deck may use at most 2 distinct colors.
            if (deckColorsUsed.size > 2) {
                errors += DeckIssue.Error(
                    "Mix leader allows at most 2 colors, but deck uses: ${deckColorsUsed.joinToString()}."
                )
            }
        } else {
            // Normal leader: every deck color must be a subset of leader colors.
            if (!deckColorsUsed.all { it in leaderColors }) {
                errors += DeckIssue.Error("Deck contains colors not included in Leader (${leader.color}).")
            }
        }

        // -------------------------
        // Rule: Max 4 copies per card code/number
        // -------------------------
        // Card.code is expected to be a stable identifier like "OP01-XXX".
        val copiesByNumber = mutableMapOf<String, Int>()
        deckCards.forEach { (card, qty) ->
            val num = (card.code ?: "").trim()
            if (num.isNotEmpty()) {
                copiesByNumber[num] = (copiesByNumber[num] ?: 0) + qty
            }
        }

        copiesByNumber.forEach { (num, count) ->
            if (count > 4) {
                errors += DeckIssue.Error("Too many copies: $num has $count copies (max 4).")
            }
        }

        // -------------------------
        // Rule: Forbidden card list (banlist)
        // -------------------------
        copiesByNumber.keys.forEach { num ->
            if (num in forbiddenCardNumbers) {
                errors += DeckIssue.Error("Forbidden card in deck: $num")
            }
        }

        // -------------------------
        // Warning: Deck incomplete (only when exact-50 is not required)
        // -------------------------
        if (!requireExactly50 && totalMain < 50) {
            warnings += DeckIssue.Warning("Deck incomplete: $totalMain/50")
        }

        val legal = errors.isEmpty()
        return DeckLegalityResult(legal, errors, warnings)
    }

    /**
     * Parses a color string into a normalized set of color tokens.
     *
     * Accepts common formats such as:
     * - "Red"
     * - "Red/Green"
     * - "Blue & Purple"
     * - "RED, GREEN"
     *
     * @return Set of lowercased color tokens, excluding blanks.
     */
    private fun parseColors(raw: String?): Set<String> {
        return (raw ?: "")
            .split("/", "&", ",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}