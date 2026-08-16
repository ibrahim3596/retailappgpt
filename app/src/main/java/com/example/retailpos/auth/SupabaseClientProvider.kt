package com.example.retailpos.auth

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth

object SupabaseClientProvider {
    private val supabaseUrl = try { BuildConfig.SUPABASE_URL } catch (e: Throwable) { "" }
    private val supabaseKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Throwable) { "" }

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl.ifBlank { "https://placeholder.supabase.co" },
            supabaseKey = supabaseKey.ifBlank { "placeholder" }
        ) {
            install(Auth) {
                scheme = "retailpos"
                host = "login-callback"
            }
        }
    }
}
