package com.koi.thepiece.ui.screens.Loginscreen

sealed interface LoginUiState {
    data object CheckingSession : LoginUiState

    data object Loading : LoginUiState
    data class NeedAuth(val reason: String? = null) : LoginUiState // expired / not_found
    data class ShowForm(val mode: AuthMode) : LoginUiState         // login/register form visible
    data class Error(val message: String) : LoginUiState


}

enum class AuthMode { LOGIN, REGISTER }