package com.koi.thepiece.scenemanagement

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable
sealed interface Route : NavKey {
    @Serializable data object Menu : Route
    @Serializable data object Catalog : Route
    @Serializable data object Scan : Route
    @Serializable data object Settings : Route
    @Serializable data object DeckList : Route
    @Serializable data object DeckBuilderLeader : Route
    @Serializable data object DeckBuilderLeaderDeck : Route

    @Serializable data object OCRScan : Route
    @Serializable data object LoginScreen : Route

}
