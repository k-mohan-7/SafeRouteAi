package com.saferoute.ai.data.repository

import com.saferoute.ai.data.local.SessionDataStore
import com.saferoute.ai.data.remote.ApiService
import com.saferoute.ai.data.remote.dto.LoginRequest
import com.saferoute.ai.data.remote.dto.RegisterRequest
import com.saferoute.ai.domain.model.UserProfile
import com.saferoute.ai.domain.model.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val sessionDataStore: SessionDataStore
) {
    suspend fun login(email: String, password: String): Result<UserProfile> = runCatching {
        val response = api.login(LoginRequest(email, password))
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Login failed" })
        }
        val data = response.data
        sessionDataStore.saveSession(data.userId, data.fullname)
        UserProfile(data.userId, data.fullname, email, null, 0)
    }

    suspend fun register(
        fullname: String,
        email: String,
        phone: String?,
        password: String,
        confirmPassword: String
    ): Result<UserProfile> = runCatching {
        val response = api.register(
            RegisterRequest(fullname, email, phone?.ifBlank { null }, password, confirmPassword)
        )
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Registration failed" })
        }
        sessionDataStore.saveSession(response.data.userId, fullname)
        UserProfile(response.data.userId, fullname, email, phone, 0)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        runCatching { api.logout() }
        sessionDataStore.clearSession()
    }

    suspend fun getProfile(): Result<UserProfile> = runCatching {
        val response = api.getProfile()
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to load profile" })
        }
        response.data.toDomain()
    }

    fun isLoggedIn() = sessionDataStore.userId
}
