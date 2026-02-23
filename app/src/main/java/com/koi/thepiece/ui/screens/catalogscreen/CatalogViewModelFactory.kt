package com.koi.thepiece.ui.screens.catalogscreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.koi.thepiece.AppGraph
import com.koi.thepiece.ui.screens.Loginscreen.LoginViewModel

/**
 * Factory for creating CatalogViewModel instances.
 *
 * Responsibilities:
 * - Inject Application (required for AndroidViewModel)
 * - Retrieve TokenStore from AppGraph
 * - Construct CatalogViewModel with required dependencies
 *
 * This avoids direct dependency creation inside Composables
 * and keeps ViewModel construction centralized.
 */
class CatalogViewModelFactory(
    private val app: Application,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            // Dependency resolved through AppGraph (service locator pattern)
            val tokenStore = AppGraph.provideTokenStore(context)
            return CatalogViewModel(app , tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Factory for creating LoginViewModel instances.
 *
 * Responsibilities:
 * - Retrieve TokenStore dependency
 * - Construct LoginViewModel with injected TokenStore
 *
 * LoginViewModel does not require Application context,
 * so it extends ViewModel instead of AndroidViewModel.
 */
class LoginViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val tokenStore = AppGraph.provideTokenStore(context)
            return LoginViewModel(tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

