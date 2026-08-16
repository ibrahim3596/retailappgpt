package com.example.retailpos.auth

import com.example.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

object SupabaseClientProvider {
    const val AUTH_CALLBACK_URL = "retailpos://login-callback"

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
            supabaseKey.isNotBlank() &&
            supabaseUrl != "https://placeholder.invalid" &&
            supabaseKey != "CONFIGURE_ME"

    val client by lazy {
        require(isConfigured) {
            "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to the app environment."
        }

        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth) {
                scheme = "retailpos"
                host = "login-callback"
            }
        }
    }
}
