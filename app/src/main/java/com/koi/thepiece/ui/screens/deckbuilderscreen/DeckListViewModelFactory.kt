package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application

class DeckListViewModelFactory(
    private val app: Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckListViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}