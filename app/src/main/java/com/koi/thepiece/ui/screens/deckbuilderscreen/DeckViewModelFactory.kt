package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.koi.thepiece.AppGraph

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
