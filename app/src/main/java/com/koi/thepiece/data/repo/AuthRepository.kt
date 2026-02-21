package com.koi.thepiece.data.repo

import com.koi.thepiece.data.api.AuthApi
import com.koi.thepiece.data.api.CatalogApi
import com.koi.thepiece.data.api.dto.*


class AuthRepository(
    val api: AuthApi,
) {
    suspend fun checkSession(token: String): Result<SessionCheckDto> =
        runCatching { api.sessionCheck(token) }

    suspend fun login(email: String, password: String): Result<LoginDto> =
        runCatching { api.login(LoginBody(email, password)) }

    suspend fun register(email: String, password: String): Result<RegisterDto> =
        runCatching { api.register(RegisterBody(email, password)) }
}