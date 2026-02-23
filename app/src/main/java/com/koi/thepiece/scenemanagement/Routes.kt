package com.koi.thepiece.scenemanagement

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Sealed navigation route definitions for the application.
 *
 * Each object represents a unique navigation destination.
 *
 * Design characteristics:
 * - Uses sealed interface to enforce compile-time safety.
 * - Implements NavKey for compatibility with NavDisplay.
 * - Annotated with @Serializable to support state restoration
 *   and navigation state persistence if required.
 *
 * This centralized routing structure ensures:
 * - Type-safe navigation
 * - No string-based route errors
 * - Clear documentation of all available screens
 */
@Serializable
sealed interface Route : NavKey {

    /** Main menu screen after successful login. */
    @Serializable
    data object Menu : Route

    /** Card catalogue screen (browse, filter, completion tracking). */
    @Serializable
    data object Catalog : Route

    /** Generic scan route (if legacy/manual scan is used). */
    @Serializable
    data object Scan : Route

    /** Settings configuration screen. */
    @Serializable
    data object Settings : Route

    /** Deck list screen displaying owned decks. */
    @Serializable
    data object DeckList : Route

    /** Deck builder screen for selecting a leader card. */
    @Serializable
    data object DeckBuilderLeader : Route

    /** Deck builder screen for editing card composition. */
    @Serializable
    data object DeckBuilderLeaderDeck : Route

    /** OCR-based card scanning screen. */
    @Serializable
    data object OCRScan : Route

    /** Login screen (entry point of application). */
    @Serializable
    data object LoginScreen : Route
}