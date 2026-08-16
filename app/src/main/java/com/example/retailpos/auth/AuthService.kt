package com.example.retailpos.auth

import kotlinx.coroutines.flow.Flow

interface AuthService {
    val authState: Flow<AuthState>
    suspend fun loginWithGoogle(): Result<Unit>
    suspend fun loginWithEmail(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun restoreSession(): Result<Unit>
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
