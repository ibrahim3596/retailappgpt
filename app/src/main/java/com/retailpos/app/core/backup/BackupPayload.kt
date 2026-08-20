package com.retailpos.app.core.backup

import java.security.MessageDigest
import org.json.JSONObject

/** JSON envelope used by future file export/import. Data stays sectioned so unknown sections can be skipped safely. */
data class BackupPayload(
    val manifest: BackupManifest,
    val sections: Map<String, JSONObject>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("manifest", manifest.toJson())
        val data = JSONObject()
        sections.forEach { (name, value) -> data.put(name, value) }
        put("data", data)
        put("checksum", sha256(data.toString()))
    }

    companion object {
        fun fromJson(json: JSONObject): BackupPayload {
            val manifest = BackupManifest.fromJson(json.getJSONObject("manifest"))
            val data = json.getJSONObject("data")
            val expected = json.optString("checksum")
            require(expected == sha256(data.toString())) { "Backup checksum mismatch; import aborted." }
            val sections = buildMap {
                for (index in 0 until data.length()) {
                    val key = data.names()?.getString(index) ?: continue
                    put(key, data.getJSONObject(key))
                }
            }
            return BackupPayload(manifest, sections)
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
