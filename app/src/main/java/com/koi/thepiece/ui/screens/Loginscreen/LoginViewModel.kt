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
import org.json.JSONObject
import retrofit2.HttpException

/**
 * ViewModel controlling authentication workflow (session check, login, register).
 *
 * Responsibilities:
 * - Reads and writes session token via TokenStore (DataStore-backed)
 * - Calls authentication endpoints via AuthApi
 * - Exposes UI state for LoginScreen using a sealed UI state model (LoginUiState)
 * - Emits a one-shot navigation signal (goNext) when authentication succeeds
 *
 * State model:
 * - uiState is a Compose State<LoginUiState> used directly by the Composable.
 * - goNext is a SharedFlow<Unit> used as a navigation event stream.
 *
 * Security notes:
 * - Only the session token is stored locally (TokenStore).
 * - Passwords are not persisted locally.
 */
class LoginViewModel(private val tokenStore: TokenStore) : ViewModel() {

    /**
     * Auth API access resolved through AppGraph.
     * This ViewModel communicates directly with the API layer for session/login/register.
     */
    private val authApi = AppGraph.provideAuthRepository().api

    /**
     * Backing state for the Login UI.
     * Uses Compose mutableStateOf so composables can observe it directly.
     */
    private val _uiState = mutableStateOf<LoginUiState>(LoginUiState.CheckingSession)
    val uiState: State<LoginUiState> = _uiState

    /**
     * One-shot navigation signal.
     * Emitted when session is valid or when login/register succeeds.
     */
    private val _goNext = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val goNext = _goNext.asSharedFlow()

    init {
        // Attempt session restoration on startup.
        checkSession()
    }

    /**
     * Validates the locally stored session token by calling session_check endpoint.
     *
     * Flow:
     * 1) Read token from TokenStore.
     * 2) If blank -> user must authenticate.
     * 3) Else call sessionCheck(token).
     * 4) If valid -> emit goNext navigation.
     * 5) If invalid -> clear token and show NeedAuth with an appropriate message.
     */
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

    /**
     * Submits a login request to the backend.
     *
     * Validation:
     * - Requires non-empty email and password.
     *
     * Success:
     * - Stores token in TokenStore.
     * - Emits goNext to trigger navigation.
     *
     * Failure:
     * - Maps common HttpException codes to user-friendly messages.
     * - Detects offline state via IOException.
     */
    fun submitLogin(email: String, password: String) {

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password required")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            runCatching { authApi.login(LoginBody(email, password)) }
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

    /**
     * Submits a registration request to the backend.
     *
     * Validation:
     * - Requires non-empty email and password.
     *
     * Success:
     * - If register succeeds, triggers submitLogin(email, password) to auto-login.
     *
     * Failure:
     * - Attempts to parse JSON error message from server for clearer feedback.
     */
    fun submitRegister(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password required")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            runCatching { authApi.register(RegisterBody(email, password)) }
                .onSuccess { res ->
                    if (res.success) {
                        // After register, auto-login
                        submitLogin(email, password)
                    } else {
                        _uiState.value = LoginUiState.Error(res.message ?: "Register failed")
                    }
                }
                .onFailure { t ->
                    val msg = if (t is HttpException) {
                        val raw = t.response()?.errorBody()?.string()
                        raw?.let {
                            runCatching { JSONObject(it).optString("message") }.getOrNull()
                        }?.takeIf { it.isNotBlank() }
                    } else null

                    _uiState.value = LoginUiState.Error(msg ?: t.message ?: "Register failed")
                }
        }
    }

    /**
     * Switches the UI into the login form panel.
     * UI layer uses this to render the appropriate AuthPanel.
     */
    fun onLoginPressed() {
        _uiState.value = LoginUiState.ShowForm(AuthMode.LOGIN)
    }

    /**
     * Switches the UI into the registration form panel.
     */
    fun onRegisterPressed() {
        _uiState.value = LoginUiState.ShowForm(AuthMode.REGISTER)
    }

    /**
     * Returns from login/register form panel back to the initial action buttons panel.
     * State is set to NeedAuth to allow the screen to show login/register entry.
     */
    fun onBackToButtons() {
        _uiState.value = LoginUiState.NeedAuth(reason = null)
    }
}