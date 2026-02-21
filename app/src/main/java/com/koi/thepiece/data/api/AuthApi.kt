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

interface AuthApi {
    @GET("api/session_check.php")
    suspend fun sessionCheck(@Query("token") token: String): SessionCheckDto

    @POST("api/login.php")
    suspend fun login(@Body body: LoginBody): LoginDto

    @POST("api/register.php")
    suspend fun register(@Body body: RegisterBody): RegisterDto
}