package com.example.retailpos.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PasswordHasherTest {

    @Test
    fun testHashAndVerify() {
        val password = "test_password_123"
        val hash = PasswordHasher.hashPassword(password)
        
        // Verify correct password
        assertTrue(PasswordHasher.verifyPassword(password, hash))
        
        // Verify incorrect password
        assertFalse(PasswordHasher.verifyPassword("wrong_password", hash))
    }

    @Test
    fun testDifferentSalts() {
        val password = "same_password"
        val hash1 = PasswordHasher.hashPassword(password)
        val hash2 = PasswordHasher.hashPassword(password)
        
        // Hashes should be different due to random salt
        assertNotEquals(hash1, hash2)
        
        // Both should verify correctly
        assertTrue(PasswordHasher.verifyPassword(password, hash1))
        assertTrue(PasswordHasher.verifyPassword(password, hash2))
    }

    @Test
    fun testInvalidHashFormat() {
        assertFalse(PasswordHasher.verifyPassword("password", "invalid:hash:format"))
        assertFalse(PasswordHasher.verifyPassword("password", "too:few"))
        assertFalse(PasswordHasher.verifyPassword("password", ""))
    }
}
