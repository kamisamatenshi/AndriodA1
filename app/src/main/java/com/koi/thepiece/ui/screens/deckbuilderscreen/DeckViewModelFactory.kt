package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.koi.thepiece.AppGraph

/**
 * Factory for creating DeckViewModel instances.
 *
 * Responsibilities:
 * - Inject Application (required for AndroidViewModel)
 * - Retrieve TokenStore from AppGraph
 * - Construct DeckViewModel with required dependencies
 *
 * This avoids direct dependency creation inside Composables
 * and keeps ViewModel construction centralized.
 */
class DeckViewModelFactory(
    private val app: Application,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            val tokenStore = AppGraph.provideTokenStore(context)
            return DeckViewModel(app,tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
