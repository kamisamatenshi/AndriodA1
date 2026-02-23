package com.koi.thepiece.data.repo

import com.koi.thepiece.data.api.AuthApi
import com.koi.thepiece.data.api.dto.*

/**
 * Repository responsible for authentication-related operations.
 *
 * This layer abstracts the AuthApi and provides:
 * - Exception-safe API calls using Kotlin Result wrapper
 * - A clean interface for ViewModel consumption
 *
 * The repository ensures the UI layer does not interact
 * directly with Retrofit or DTO construction logic.
 */
class AuthRepository(
    val api: AuthApi,
) {

    /**
     * Validates an existing session token with the backend.
     *
     * @param token Stored session token.
     * @return Result wrapping SessionCheckDto.
     */
    suspend fun checkSession(token: String): Result<SessionCheckDto> =
        runCatching {
            api.sessionCheck(token)
        }

    /**
     * Performs user login.
     *
     * Constructs LoginBody internally to keep DTO creation
     * outside the ViewModel layer.
     *
     * @param email User email.
     * @param password User plaintext password.
     * @return Result wrapping LoginDto.
     */
    suspend fun login(email: String, password: String): Result<LoginDto> =
        runCatching {
            api.login(LoginBody(email, password))
        }

    /**
     * Registers a new user account.
     *
     * @param email User email.
     * @param password User plaintext password.
     * @return Result wrapping RegisterDto.
     */
    suspend fun register(email: String, password: String): Result<RegisterDto> =
        runCatching {
            api.register(RegisterBody(email, password))
        }
}