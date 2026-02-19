package com.koi.thepiece.scenemanagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.screens.MenuScreen
import com.koi.thepiece.ui.screens.Scan
import com.koi.thepiece.ui.screens.SettingsScreen
import com.koi.thepiece.ui.screens.catalogscreen.CatalogScreen

@Composable
fun AppNavGraph(
    imageLoader: ImageLoader,
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val backStack = remember { mutableStateListOf<Route>(Route.Menu) }

    NavDisplay(
        backStack = backStack,
        entryProvider = { key ->
            when (key) {

                Route.Menu -> NavEntry(key) {
                    MenuScreen(
                        audioManager = audioManager,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onGoCatalog = { backStack.add(Route.Catalog) },
                        onGoScanner = { backStack.add(Route.Scan) },
                        onGoSettings = { backStack.add(Route.Settings) },
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
