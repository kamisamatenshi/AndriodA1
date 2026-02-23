package com.koi.thepiece.ui.screens.Loginscreen

/**
 * Represents all possible UI states for the Login screen.
 *
 * This sealed interface ensures:
 * - Exhaustive `when` handling in Composables
 * - Clear separation of authentication flow states
 * - Strongly typed UI logic (no ambiguous flags)
 *
 * The Login screen should render purely based on this state.
 */
sealed interface LoginUiState {

    /**
     * App is checking whether a valid session already exists.
     *
     * Typically triggered on startup.
     * UI may show a loading indicator or short message.
     */
    data object CheckingSession : LoginUiState

    /**
     * Authentication request is in progress.
     *
     * Used during login or registration network calls.
     * UI should show a progress indicator.
     */
    data object Loading : LoginUiState

    /**
     * User must authenticate.
     *
     * @param reason Optional explanation:
     * - "expired"
     * - "not_found"
     * - custom backend message
     */
    data class NeedAuth(val reason: String? = null) : LoginUiState

    /**
     * Displays authentication form panel.
     *
     * @param mode Determines which form is visible (Login or Register).
     */
    data class ShowForm(val mode: AuthMode) : LoginUiState

    /**
     * An error occurred during authentication.
     *
     * @param message User-readable error message.
     */
    data class Error(val message: String) : LoginUiState
}

/**
 * Indicates which authentication form is active.
 *
 * - LOGIN → Existing user sign-in.
 * - REGISTER → New user account creation.
 */
enum class AuthMode {
    LOGIN,
    REGISTER
}