package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import android.content.Context
import com.koi.thepiece.AppGraph

/**
 * Factory for creating DeckListViewModel instances.
 *
 * Responsibilities:
 * - Inject Application (required for AndroidViewModel)
 * - Retrieve TokenStore from AppGraph
 * - Construct DeckListViewModel with required dependencies
 *
 * This avoids direct dependency creation inside Composables
 * and keeps ViewModel construction centralized.
 */
class DeckListViewModelFactory(
    private val app: Application,
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckListViewModel::class.java)) {
            val tokenStore = AppGraph.provideTokenStore(context)
            @Suppress("UNCHECKED_CAST")
            return DeckListViewModel(app,tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}