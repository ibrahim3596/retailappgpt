package com.retailpos.app.core.backup

import android.content.Context
import androidx.core.content.edit
import com.retailpos.app.data.RetailDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

object DatabaseBackupManager {
    private const val DATABASE_FILE = "retailpos.db"
    private const val PAYLOAD_MAGIC = "RPOS-SNAPSHOT-1"
    private const val PREFS = "retailpos_backup"
    private const val LAST_BACKUP_AT = "last_backup_at"
    private const val LOCAL_STORE_ID = "local-store"
    private const val CURRENT_SCHEMA = 25
    private val operationInProgress = AtomicBoolean(false)

    suspend fun exportEncrypted(context: Context, output: OutputStream, password: CharArray): BackupManifest {
        require(operationInProgress.compareAndSet(false, true)) { "Another backup operation is already running." }
        return try {
            val snapshot = createSqliteSnapshot(context)
            val manifest = BackupManifest(
                appSchemaVersion = CURRENT_SCHEMA,
                storeId = LOCAL_STORE_ID,
                createdAt = System.currentTimeMillis(),
                sections = BackupSection.ALL
            )
            val payload = pack(manifest, snapshot)
            val encrypted = EncryptedBackupCodec.encrypt(payload, password)
            output.use { it.write(encrypted) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(LAST_BACKUP_AT, manifest.createdAt) }
            manifest
        } finally {
            operationInProgress.set(false)
        }
    }

    suspend fun importEncrypted(context: Context, input: InputStream, password: CharArray): BackupManifest {
        require(operationInProgress.compareAndSet(false, true)) { "Another backup operation is already running." }
        return try {
            val container = input.use { it.readBytes() }
            val payload = EncryptedBackupCodec.decrypt(container, password)
            val unpacked = unpack(payload)
            require(unpacked.manifest.appSchemaVersion == CURRENT_SCHEMA) {
                "Backup schema ${unpacked.manifest.appSchemaVersion} is incompatible with this app (expected $CURRENT_SCHEMA)."
            }
            require(unpacked.manifest.storeId == LOCAL_STORE_ID) {
                "This backup belongs to another store. Import it from the matching store installation."
            }
            require(sha256(unpacked.databaseBytes) == unpacked.checksum) { "Backup checksum mismatch; import aborted." }
            require(isSQLiteDatabase(unpacked.databaseBytes)) { "Backup does not contain a valid SQLite database." }
            replaceDatabase(context, unpacked.databaseBytes)
            unpacked.manifest
        } finally {
            operationInProgress.set(false)
        }
    }

    fun lastBackupAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_BACKUP_AT, 0L)

    private fun createSqliteSnapshot(context: Context): ByteArray {
        val database = RetailDatabase.get(context)
        val temp = File(context.cacheDir, "retailpos-backup-${System.currentTimeMillis()}.db")
        temp.delete()
        val escapedPath = temp.absolutePath.replace("'", "''")
        try {
            val sqlite = database.openHelper.writableDatabase
            sqlite.execSQL("PRAGMA wal_checkpoint(FULL)")
            sqlite.execSQL("VACUUM INTO '$escapedPath'")
            return temp.readBytes()
        } finally {
            temp.delete()
        }
    }

    private fun replaceDatabase(context: Context, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Backup database is empty" }
        RetailDatabase.closeForRestore()
        val databaseFile = context.getDatabasePath(DATABASE_FILE)
        databaseFile.parentFile?.mkdirs()
        val restoreFile = File(databaseFile.parentFile, "$DATABASE_FILE.restore")
        val previousFile = File(databaseFile.parentFile, "$DATABASE_FILE.pre-restore")
        restoreFile.writeBytes(bytes)
        check(restoreFile.length() == bytes.size.toLong()) { "Restore copy was incomplete" }
        if (previousFile.exists()) previousFile.delete()

        val hadExisting = databaseFile.exists()
        if (hadExisting) {
            check(databaseFile.renameTo(previousFile)) { "Could not protect the existing database before restore" }
        }
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()

        try {
            check(restoreFile.renameTo(databaseFile)) { "Could not activate restored database" }
            previousFile.delete()
        } catch (error: Exception) {
            databaseFile.delete()
            if (hadExisting && previousFile.exists()) previousFile.renameTo(databaseFile)
            restoreFile.delete()
            throw error
        } finally {
            restoreFile.delete()
        }
    }

    private data class Unpacked(val manifest: BackupManifest, val checksum: String, val databaseBytes: ByteArray)

    private fun pack(manifest: BackupManifest, databaseBytes: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { data ->
            data.writeUTF(PAYLOAD_MAGIC)
            val manifestBytes = manifest.toJson().toString().toByteArray(Charsets.UTF_8)
            data.writeInt(manifestBytes.size)
            data.write(manifestBytes)
            data.writeUTF(sha256(databaseBytes))
            data.writeInt(databaseBytes.size)
            data.write(databaseBytes)
        }
        output.toByteArray()
    }

    private fun unpack(payload: ByteArray): Unpacked = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        require(input.readUTF() == PAYLOAD_MAGIC) { "Unsupported backup payload" }
        val manifestLength = input.readInt()
        require(manifestLength in 1..1_000_000) { "Invalid backup manifest" }
        val manifestJson = ByteArray(manifestLength).also(input::readFully)
        val manifest = BackupManifest.fromJson(JSONObject(String(manifestJson, Charsets.UTF_8)))
        val checksum = input.readUTF()
        require(checksum.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid backup checksum" }
        val databaseLength = input.readInt()
        require(databaseLength in 16..500_000_000) { "Backup database payload is invalid or too large" }
        val databaseBytes = ByteArray(databaseLength).also(input::readFully)
        Unpacked(manifest, checksum, databaseBytes)
    }

    private fun isSQLiteDatabase(bytes: ByteArray): Boolean =
        bytes.size >= 16 && bytes.copyOfRange(0, 16).contentEquals("SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
