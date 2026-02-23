package com.koi.thepiece.ui.screens.Loginscreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.koi.thepiece.data.api.dto.LoginBody
import com.koi.thepiece.data.api.dto.RegisterBody
import com.koi.thepiece.data.local.TokenStore
import kotlinx.coroutines.flow.firstOrNull
import okio.IOException
import retrofit2.HttpException

class LoginViewModel(private val tokenStore: TokenStore) : ViewModel() {

    private val authApi = AppGraph.provideAuthRepository().api

    private val _uiState = mutableStateOf<LoginUiState>(LoginUiState.CheckingSession)
    val uiState: State<LoginUiState> = _uiState

    // one-shot navigation signal
    private val _goNext = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val goNext = _goNext.asSharedFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()

            if (token.isBlank()) {
                _uiState.value = LoginUiState.NeedAuth(reason = null)
                return@launch
            }

            runCatching { authApi.sessionCheck(token) }
                .onSuccess { res ->
                    if (res.success && res.valid) {
                        _goNext.tryEmit(Unit)
                    } else {

                        tokenStore.clearToken()

                        val message = when (res.reason) {
                            "expired" -> "Your session has expired. Please login again."
                            "not_found" -> "Session invalid. Please login again."
                            else -> "Authentication required."
                        }

                        _uiState.value = LoginUiState.NeedAuth(message)
                    }
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState.Error(e.message ?: "Session check failed")
                }
        }
    }
    
    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Email is required"

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(trimmed)) return "Invalid email format"

        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Password is required"
        if (password.length < 8) return "Password must be at least 8 characters"

        return null
    }

    private fun validateCredentials(email: String, password: String): String? {
        return validateEmail(email) ?: validatePassword(password)
    }

    fun submitLogin(email: String, password: String) {

        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            _uiState.value = LoginUiState.Error(validationError)
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            runCatching { authApi.login(LoginBody(email.trim(), password)) }
                .onSuccess { res ->

                    if (res.success && !res.token.isNullOrBlank()) {
                        tokenStore.saveToken(res.token!!)
                        _goNext.emit(Unit)
                    } else {
                        _uiState.value =
                            LoginUiState.Error(res.message ?: "Login failed")
                    }
                }
                .onFailure { throwable ->

                    when (throwable) {

                        is HttpException -> {
                            when (throwable.code()) {
                                401 -> _uiState.value =
                                    LoginUiState.Error("Incorrect email or password")

                                400 -> _uiState.value =
                                    LoginUiState.Error("Invalid email or password")

                                500 -> _uiState.value =
                                    LoginUiState.Error("Server error. Please try again.")

                                else -> _uiState.value =
                                    LoginUiState.Error("Login failed")
                            }
                        }

                        is IOException -> {
                            _uiState.value =
                                LoginUiState.Error("No internet connection")
                        }

                        else -> {
                            _uiState.value =
                                LoginUiState.Error("Unexpected error occurred")
                        }
                    }
                }
        }
    }

    fun submitRegister(email: String, password: String) {

        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            _uiState.value = LoginUiState.Error(validationError)
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            runCatching { authApi.register(RegisterBody(email.trim(), password)) }
                .onSuccess { res ->
                    if (res.success) {
                        // After register, auto-login
                        submitLogin(email, password)
                    } else {
                        _uiState.value =
                            LoginUiState.Error(res.message ?: "Register failed")
                    }
                }
                .onFailure { throwable ->

                    when (throwable) {

                        is HttpException -> {
                            when (throwable.code()) {

                                //Suppose to have this error when same email/username
                                409 -> _uiState.value =
                                    LoginUiState.Error("An account with this email already exists")

                                //Bad Request
                                400 -> _uiState.value =
                                    LoginUiState.Error("Invalid email or password")

                                500 -> _uiState.value =
                                    LoginUiState.Error("Server error. Please try again.")

                                else -> _uiState.value =
                                    LoginUiState.Error("Register failed")
                            }
                        }

                        is IOException -> {
                            _uiState.value =
                                LoginUiState.Error("No internet connection")
                        }

                        else -> {
                            _uiState.value =
                                LoginUiState.Error("Unexpected error occurred")
                        }
                    }
                }
        }
    }
    fun onLoginPressed() {
        _uiState.value = LoginUiState.ShowForm(AuthMode.LOGIN)
    }

    fun onRegisterPressed() {
        _uiState.value = LoginUiState.ShowForm(AuthMode.REGISTER)
    }

    fun onBackToButtons() {
        _uiState.value = LoginUiState.NeedAuth(reason = null)
    }
}