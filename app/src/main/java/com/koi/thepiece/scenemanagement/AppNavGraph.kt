package com.koi.thepiece.scenemanagement

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.LeaderDeckBuildScreen
import com.koi.thepiece.ui.screens.MenuScreen
import com.koi.thepiece.ui.screens.OnePieceCardScan
import com.koi.thepiece.ui.screens.Scan
import com.koi.thepiece.ui.screens.SettingsScreen
import com.koi.thepiece.ui.screens.catalogscreen.CatalogScreen
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck.DeckCardBuildScreen
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListScreen
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModelFactory
import com.koi.thepiece.ui.screens.Loginscreen.LoginScreen
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModel
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModelFactory
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListViewModel
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListViewModelFactory

/**
 * Application-level navigation graph implemented using a manual back stack.
 *
 * This composable defines all top-level routes and their screen content,
 * and centralizes navigation behavior (push/pop) in one location.
 *
 * Navigation approach:
 * - A MutableStateList<Route> is used as a lightweight back stack.
 * - Each navigation action pushes a new Route onto the stack.
 * - Back navigation pops the last Route when possible.
 *
 * Shared dependencies:
 * - ImageLoader is injected for consistent image loading/caching across screens.
 * - AudioManager is injected to ensure background music and sound effects remain consistent.
 * - Theme state is injected to allow Settings-driven theme toggling.
 *
 * Back handling:
 * - A simple debounce (350ms) is applied on some screens to avoid rapid double-press issues.
 */
@Composable
fun AppNavGraph(
    imageLoader: ImageLoader,
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    /**
     * Manual navigation back stack.
     * The initial route is LoginScreen.
     */
    val backStack = remember { mutableStateListOf<Route>(Route.LoginScreen) }

    /**
     * Application context used for ViewModel factories that require Application.
     */
    val app = LocalContext.current.applicationContext as Application

    /**
     * Timestamp used to debounce back presses and avoid accidental double-pop.
     */
    var lastBackMs by remember { mutableLongStateOf(0L) }

    /**
     * NavDisplay renders the current screen from the top of the backStack.
     * entryProvider maps each Route to its corresponding UI content.
     */
    NavDisplay(
        backStack = backStack,
        /**
         * Provides the screen content for each Route.
         * Each NavEntry defines what composable should be shown for that route.
         */
        entryProvider = { key ->
            when (key) {
                /**
                 * Login screen route.
                 * On successful login, navigation proceeds to the main menu.
                 */
                Route.LoginScreen -> NavEntry(key) {
                    LoginScreen(
                        audioManager = audioManager,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onGoToMainmenu = { backStack.add(Route.Menu)}
                    )
                }
                /**
                 * Main menu route.
                 * Provides entry points to core features: Decks, Catalogue, Scanner, Settings.
                 * Logout clears the back stack to prevent returning to authenticated screens.
                 */
                Route.Menu -> NavEntry(key) {
                    MenuScreen(
                        audioManager = audioManager,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onGoDeckList = { backStack.add(Route.DeckList) },
                        onGoCatalog = { backStack.add(Route.Catalog) },
                        onGoScanner = { backStack.add(Route.OCRScan) },
                        onGoSettings = { backStack.add(Route.Settings) },

                        onLogoutConfirmed = {
                            backStack.clear()
                            backStack.add(Route.LoginScreen)  // go to login screen
                        }

                    )
                }
                /**
                 * Catalogue route.
                 * Uses back debouncing to avoid rapid double press removing two routes.
                 */
                Route.Catalog -> NavEntry(key) {
                    CatalogScreen(
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        imageLoader = imageLoader,
                        audio = audioManager
                    )
                }
                /**
                 * Legacy / alternate scan route (if still used).
                 * Pops the back stack when back is pressed.
                 */
                Route.Scan -> NavEntry(key) {
                    Scan(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                /**
                 * OCR scanner route.
                 *
                 * Creates a CatalogViewModel instance (via factory) to reuse catalogue logic
                 * for card lookup after OCR detection.
                 *
                 * onCodeDetected is currently used for logging and can be extended to:
                 * - auto-navigate to card detail view
                 * - auto-run catalogue search
                 */
                Route.OCRScan -> NavEntry(key) {
                    val app = LocalContext.current.applicationContext as Application
                    val context = LocalContext.current
                    val scanViewModel: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(app,context))
                    OnePieceCardScan(
                        audioManager = audioManager,
                        imageLoader = imageLoader,
                        viewModel = scanViewModel,
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onCodeDetected = { code ->
                            Log.d("OCRScanRoute", "Card code detected: $code")
                        }
                    )
                }

                /**
                 * Settings route.
                 * Provides access to configuration options (theme, audio, etc.).
                 */
                Route.Settings -> NavEntry(key) {
                    SettingsScreen(
                        audio = audioManager,
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                /**
                 * Deck list route.
                 *
                 * Creates:
                 * - DeckListViewModel for deck listing and server sync
                 * - DeckViewModel as a shared VM to hold selected deck state when navigating
                 *
                 * refreshDecksFromServer() is triggered before screen content renders,
                 * ensuring the list reflects the latest server-owned decks.
                 */
                Route.DeckList -> NavEntry(key) {
                    val app = LocalContext.current.applicationContext as Application
                    val context = LocalContext.current
                    val deckListVm: DeckListViewModel = viewModel(factory = DeckListViewModelFactory(app,context))
                    val deckVm: DeckViewModel = viewModel(factory = DeckViewModelFactory(app,context))


                    DeckListScreen(
                        vm = deckListVm,
                        deckVm = deckVm,
                        audio = audioManager,
                        imageLoader = imageLoader,
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onGoCreateNewDeck = { backStack.add(Route.DeckBuilderLeader) },
                        onOpenDeck = { deckId ->
                            deckVm.loadDeck(deckId)                 // load saved deck into shared VM
                            backStack.add(Route.DeckBuilderLeaderDeck) // jump straight into deck screen
                        }
                    )
                }

                /**
                 * Deck builder leader-selection route.
                 * Sets the selected leader and then navigates to the deck card selection screen.
                 */
                Route.DeckBuilderLeader -> NavEntry(key) {
                    val context = LocalContext.current
                    val deckVm: DeckViewModel = viewModel(factory = DeckViewModelFactory(app,context))
                    LeaderDeckBuildScreen(
                        vm = deckVm,
                        audio = audioManager,
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        imageLoader = imageLoader,
                        onGoCreateNewDeck = { leaderCard ->
                            deckVm.setSelectedLeader(leaderCard)
                            backStack.add(Route.DeckBuilderLeaderDeck)
                        }
                    )
                }

                /**
                 * Deck builder main route (deck composition screen).
                 * Uses the shared DeckViewModel to maintain deck state across navigation.
                 */
                Route.DeckBuilderLeaderDeck -> NavEntry(key) {
                    val context = LocalContext.current
                    val deckVm: DeckViewModel = viewModel(factory = DeckViewModelFactory(app,context))
                    DeckCardBuildScreen(
                        vm = deckVm,
                        audio = audioManager,
                        onBack = let@{
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastBackMs < 350L) return@let // ignore rapid double press
                            lastBackMs = now

                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        imageLoader = imageLoader
                    )
                }
            }
        },

        /**
         * Global back handler for the navigation system.
         * Pops the current route if more than one route exists.
         *
         * @return true if back was handled, false if at root.
         */
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
                true
            } else {
                false
            }
        }
    )
}
