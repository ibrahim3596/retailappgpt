package com.retailpos.app.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test
    fun encryptedPayloadRoundTrips() {
        val payload = "retailpos-backup-test".toByteArray()
        val encoded = EncryptedBackupCodec.encrypt(payload, "correct-horse".toCharArray())
        val decoded = EncryptedBackupCodec.decrypt(encoded, "correct-horse".toCharArray())
        assertArrayEquals(payload, decoded)
    }

    @Test
    fun wrongPasswordIsRejected() {
        val encoded = EncryptedBackupCodec.encrypt("secret".toByteArray(), "correct-horse".toCharArray())
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encoded, "wrong-password".toCharArray())
        }
    }

    @Test
    fun corruptedContainerIsRejected() {
        val encoded = EncryptedBackupCodec.encrypt("secret".toByteArray(), "correct-horse".toCharArray())
        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 0x01).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encoded, "correct-horse".toCharArray())
        }
    }
}
