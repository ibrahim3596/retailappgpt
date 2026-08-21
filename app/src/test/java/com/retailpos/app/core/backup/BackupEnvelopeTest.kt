package com.retailpos.app.core.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupEnvelopeTest {
    @Test
    fun manifestRoundTrips() {
        val manifest = BackupManifest(
            appSchemaVersion = 24,
            storeId = "local-store",
            createdAt = 123L,
            sections = listOf("PRODUCTS", "SALES")
        )
        assertEquals(manifest, BackupManifest.fromJson(manifest.toJson()))
    }

    @Test
    fun payloadRoundTripsWithChecksum() {
        val payload = BackupPayload(
            BackupManifest(24, "local-store", 123L, listOf("PRODUCTS")),
            mapOf("PRODUCTS" to JSONObject().put("count", 2))
        )
        val restored = BackupPayload.fromJson(payload.toJson())
        assertEquals(2, restored.sections["PRODUCTS"]?.getInt("count"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tamperedPayloadIsRejected() {
        val payload = BackupPayload(
            BackupManifest(24, "local-store", 123L, listOf("PRODUCTS")),
            mapOf("PRODUCTS" to JSONObject().put("count", 2))
        )
        val json = payload.toJson().put("data", JSONObject().put("PRODUCTS", JSONObject().put("count", 999)))
        BackupPayload.fromJson(json)
    }
}
