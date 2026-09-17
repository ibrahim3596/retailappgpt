package com.example.retailpos.engine.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Outcome of a single sync HTTP call. */
sealed class SyncHttpResult {
    data class Success(val bodyJson: String) : SyncHttpResult()
    data class ClientError(val httpCode: Int, val error: String?, val message: String?) : SyncHttpResult()
    data class ServerError(val httpCode: Int) : SyncHttpResult()
    data class NetworkFailure(val cause: IOException) : SyncHttpResult()
}

/**
 * Thin OkHttp wrapper for the sync server. Auth tokens are injected per call
 * by the repository; no global mutable client state.
 */
class SyncApiClient(
    private val baseUrlProvider: suspend () -> String?
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun url(path: String): String {
        val base = baseUrlProvider()?.trimEnd('/') ?: throw IOException("Sync server is not configured")
        return "$base$path"
    }

    suspend fun post(path: String, jsonBody: String, accessToken: String?, idempotencyKey: String? = null): SyncHttpResult {
        val builder = Request.Builder()
            .url(url(path))
            .post(jsonBody.toRequestBody(jsonMediaType))
        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey)
        }
        return execute(builder.build())
    }

    suspend fun get(path: String, query: Map<String, String>, accessToken: String?): SyncHttpResult {
        val qs = query.entries.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }
        val builder = Request.Builder()
            .url(url("$path?$qs"))
            .get()
        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return execute(builder.build())
    }

    private fun execute(request: Request): SyncHttpResult {
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> SyncHttpResult.Success(body)
                    response.code in 400..499 -> {
                        val parsed = SyncContracts.decode<PushResponse>(body)
                        SyncHttpResult.ClientError(
                            httpCode = response.code,
                            error = parsed?.error,
                            message = parsed?.message
                        )
                    }
                    else -> SyncHttpResult.ServerError(response.code)
                }
            }
        } catch (e: IOException) {
            SyncHttpResult.NetworkFailure(e)
        }
    }
}
