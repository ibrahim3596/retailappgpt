package com.example.retailpos.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseAuthService : AuthService {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    override suspend fun loginWithGoogle(): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(IllegalStateException("Google sign-in is not configured yet."))
        }

        return try {
            SupabaseClientProvider.client.auth.signInWith(Google)
            updateAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(IllegalStateException("Cloud authentication is not configured yet."))
        }

        return try {
            SupabaseClientProvider.client.auth.signInWith(
                io.github.jan.supabase.auth.providers.builtin.Email
            ) {
                this.email = email
                this.password = password
            }
            updateAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) {
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        }

        return try {
            SupabaseClientProvider.client.auth.signOut()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreSession(): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) {
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        }

        return try {
            updateAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateAuthState() {
        val user = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user
        if (user != null) {
            _authState.value = AuthState.Authenticated(user.id, user.email)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
