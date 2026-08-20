package com.retailpos.app.core.offline

import org.json.JSONObject

data class BackupManifest(
    val schemaVersion: Int,
    val createdAt: Long,
    val storeId: String,
    val appBuild: String
) {
    fun toJson(): String = JSONObject().apply {
        put("schemaVersion", schemaVersion)
        put("createdAt", createdAt)
        put("storeId", storeId)
        put("appBuild", appBuild)
    }.toString()

    companion object {
        fun fromJson(raw: String): BackupManifest? = runCatching {
            val json = JSONObject(raw)
            BackupManifest(
                schemaVersion = json.getInt("schemaVersion"),
                createdAt = json.getLong("createdAt"),
                storeId = json.getString("storeId"),
                appBuild = json.getString("appBuild")
            )
        }.getOrNull()
    }
}
