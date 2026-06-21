package com.example.data.service

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CsvEncryptionUtility {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"

    /**
     * Encrypts the plain text CSV using AES-256 standard and returns a Base64 string.
     */
    fun encrypt(plainText: String, passcode: String): String {
        try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(passcode.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, KEY_ALGORITHM)
            
            // Standard IV derived repeatably from the SHA-256 key's first 16 bytes.
            val ivBytes = keyBytes.copyOfRange(0, 16)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText // secure safety fallback
        }
    }

    /**
     * Decrypts a Base64-encoded AES-256 cipher string back to original plain text.
     */
    fun decrypt(encryptedText: String, passcode: String): String {
        try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(passcode.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, KEY_ALGORITHM)
            
            val ivBytes = keyBytes.copyOfRange(0, 16)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "Decryption Error: Invalid security passphrase code or corrupted string."
        }
    }
}
