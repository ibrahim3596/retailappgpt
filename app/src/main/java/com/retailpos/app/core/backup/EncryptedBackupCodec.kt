package com.retailpos.app.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptedBackupCodec {
    private const val MAGIC = "RPOS-ENC-1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MAX_ENCRYPTED_BYTES = 500_100_000

    fun encrypt(payload: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Backup password cannot be empty" }
        require(payload.isNotEmpty() && payload.size <= MAX_ENCRYPTED_BYTES) { "Backup payload is invalid or too large" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(payload)
        return ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { data ->
                data.writeUTF(MAGIC)
                data.writeInt(ITERATIONS)
                data.writeInt(salt.size)
                data.write(salt)
                data.writeInt(iv.size)
                data.write(iv)
                data.writeInt(encrypted.size)
                data.write(encrypted)
            }
            output.toByteArray()
        }
    }

    fun decrypt(container: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Backup password cannot be empty" }
        require(container.size in 32..MAX_ENCRYPTED_BYTES + 1024) { "Backup file is invalid or too large" }
        DataInputStream(ByteArrayInputStream(container)).use { input ->
            require(input.readUTF() == MAGIC) { "Not a supported RetailPOS backup file" }
            val iterations = input.readInt()
            require(iterations in 100_000..500_000) { "Unsupported backup encryption parameters" }
            val saltLength = input.readInt()
            require(saltLength == SALT_BYTES) { "Invalid backup salt" }
            val salt = ByteArray(saltLength).also(input::readFully)
            val ivLength = input.readInt()
            require(ivLength == IV_BYTES) { "Invalid backup IV" }
            val iv = ByteArray(ivLength).also(input::readFully)
            val encryptedLength = input.readInt()
            require(encryptedLength in 17..MAX_ENCRYPTED_BYTES + 32) { "Invalid encrypted backup payload" }
            val encrypted = ByteArray(encryptedLength).also(input::readFully)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            return runCatching { cipher.doFinal(encrypted) }
                .getOrElse { throw IllegalArgumentException("Incorrect backup password or damaged backup", it) }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
