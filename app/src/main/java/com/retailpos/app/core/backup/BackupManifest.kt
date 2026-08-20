package com.retailpos.app.core.backup

import org.json.JSONObject

/** Stable metadata describing an offline backup payload. */
data class BackupManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appSchemaVersion: Int,
    val storeId: String,
    val createdAt: Long,
    val sections: List<String>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("formatVersion", formatVersion)
        put("appSchemaVersion", appSchemaVersion)
        put("storeId", storeId)
        put("createdAt", createdAt)
        put("sections", org.json.JSONArray(sections))
    }

    companion object {
        const val CURRENT_FORMAT_VERSION = 1

        fun fromJson(json: JSONObject): BackupManifest {
            val formatVersion = json.optInt("formatVersion", -1)
            require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported backup format: $formatVersion" }
            val sectionsJson = json.optJSONArray("sections") ?: org.json.JSONArray()
            val sections = buildList {
                for (index in 0 until sectionsJson.length()) add(sectionsJson.getString(index))
            }
            return BackupManifest(
                formatVersion = formatVersion,
                appSchemaVersion = json.optInt("appSchemaVersion", 0),
                storeId = json.getString("storeId"),
                createdAt = json.getLong("createdAt"),
                sections = sections
            )
        }
    }
}
