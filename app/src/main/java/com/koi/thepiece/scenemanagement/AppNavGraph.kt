package com.koi.thepiece.scenemanagement

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.CreateDeck.LeaderDeckBuildScreen
import com.koi.thepiece.ui.screens.MenuScreen
import com.koi.thepiece.ui.screens.OnePieceCardScan
import com.koi.thepiece.ui.screens.Scan
import com.koi.thepiece.ui.screens.SettingsScreen
import com.koi.thepiece.ui.screens.catalogscreen.CatalogScreen

import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckCardBuildScreen
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListScreen
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koi.thepiece.ui.screens.Loginscreen.LoginScreen

import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModel
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModelFactory
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListViewModel
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckListViewModelFactory

@Composable
fun AppNavGraph(
    imageLoader: ImageLoader,
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val backStack = remember { mutableStateListOf<Route>(Route.LoginScreen) }

    // For deck to use the shared view model for easier control
    val app = LocalContext.current.applicationContext as Application
    val deckVm: DeckViewModel = viewModel(factory = DeckViewModelFactory(app))
    val deckListVm: DeckListViewModel = viewModel(factory = DeckListViewModelFactory(app))

    NavDisplay(
        backStack = backStack,
        entryProvider = { key ->
            when (key) {
                Route.LoginScreen -> NavEntry(key) {
                    LoginScreen(
                        audioManager = audioManager,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onGoToMainmenu = { backStack.add(Route.Menu)}
                    )
                }

                Route.Menu -> NavEntry(key) {
                    MenuScreen(
                        audioManager = audioManager,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onGoDeckList = { backStack.add(Route.DeckList) },
                        onGoCatalog = { backStack.add(Route.Catalog) },
                        onGoScanner = { backStack.add(Route.OCRScan) },
                        onGoSettings = { backStack.add(Route.Settings) },
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                Route.Catalog -> NavEntry(key) {
                    CatalogScreen(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        imageLoader = imageLoader,
                        audio = audioManager
                    )
                }

                Route.Scan -> NavEntry(key) {
                    Scan(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                Route.OCRScan -> NavEntry(key) {
                    val app = LocalContext.current.applicationContext as Application
                    val scanViewModel: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(app))
                    OnePieceCardScan(
                        audioManager = audioManager,
                        imageLoader = imageLoader,
                        viewModel = scanViewModel,
                        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                        onCodeDetected = { code ->
                            Log.d("OCRScanRoute", "Card code detected: $code")
                            // Optional: navigate to detail screen
                        }
                    )
                }


                Route.Settings -> NavEntry(key) {
                    SettingsScreen(
                        audio = audioManager,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                Route.DeckList -> NavEntry(key) {
                    val app = LocalContext.current.applicationContext as Application
                    val deckListVm: DeckListViewModel = viewModel(factory = DeckListViewModelFactory(app))

                    DeckListScreen(
                        vm = deckListVm,
                        deckVm = deckVm,
                        audio = audioManager,
                        imageLoader = imageLoader,
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        onGoCreateNewDeck = { backStack.add(Route.DeckBuilderLeader) },
                        onOpenDeck = { deckId ->
                            deckVm.loadDeck(deckId)                 // load saved deck into shared VM
                            backStack.add(Route.DeckBuilderLeaderDeck) // jump straight into deck screen
                        }
                    )
                }

                Route.DeckBuilderLeader -> NavEntry(key) {
                    LeaderDeckBuildScreen(
                        vm = deckVm,
                        audio = audioManager,
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        imageLoader = imageLoader,
                        onGoCreateNewDeck = { leaderCard ->
                            deckVm.setSelectedLeader(leaderCard)
                            backStack.add(Route.DeckBuilderLeaderDeck)
                        }
                    )
                }

                Route.DeckBuilderLeaderDeck -> NavEntry(key) {
                    DeckCardBuildScreen(
                        vm = deckVm,
                        audio = audioManager,
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        imageLoader = imageLoader
                    )
                }
            }
        },

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
