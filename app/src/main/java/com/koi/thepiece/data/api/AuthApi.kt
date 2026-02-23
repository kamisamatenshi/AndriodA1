package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.LoginBody
import com.koi.thepiece.data.api.dto.LoginDto
import com.koi.thepiece.data.api.dto.RegisterBody
import com.koi.thepiece.data.api.dto.RegisterDto
import com.koi.thepiece.data.api.dto.SessionCheckDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API contract for authentication-related endpoints.
 *
 * This interface defines all network operations related to:
 * - Session validation
 * - User login
 * - User registration
 *
 * All endpoints communicate with the backend PHP authentication services.
 */
interface AuthApi {

    /**
     * Verifies whether a given session token is valid.
     *
     * Used during app startup or protected navigation checks
     * to confirm whether the user session is still active.
     *
     * @param token Session token previously issued by the server.
     * @return SessionCheckDto containing validity status and user information.
     */
    @GET("api/session_check.php")
    suspend fun sessionCheck(
        @Query("token") token: String
    ): SessionCheckDto

    /**
     * Authenticates a user using email and password credentials.
     *
     * The backend verifies the password hash and, if valid,
     * generates a session token stored in the `user_sessions` table.
     *
     * @param body LoginBody containing email and plaintext password.
     * @return LoginDto containing authentication result and session token.
     */
    @POST("api/login.php")
    suspend fun login(
        @Body body: LoginBody
    ): LoginDto

    /**
     * Registers a new user account.
     *
     * The backend:
     * - Validates email format
     * - Enforces password requirements
     * - Hashes the password securely
     * - Inserts a new user record
     *
     * @param body RegisterBody containing email and plaintext password.
     * @return RegisterDto containing registration result and user ID.
     */
    @POST("api/register.php")
    suspend fun register(
        @Body body: RegisterBody
    ): RegisterDto
}