package com.example.data.database

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object DatabaseEncryptionBackupManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_SIZE_BYTE = 12
    private const val SALT_SIZE_BYTE = 16
    private const val KEY_GEN_ITERATIONS = 10000
    private const val KEY_LENGTH_BIT = 256

    data class BackupMetadata(
        val fileName: String,
        val filePath: String,
        val sizeBytes: Long,
        val timestamp: Long
    )

    suspend fun exportAndEncryptDatabase(
        context: Context,
        passphrase: String,
        dbName: String = "finance_tracker_db"
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Database file not found: ${dbFile.absolutePath}"))
            }

            // Ensure checkpoint / WAL flush if needed by reading bytes
            val dbBytes = dbFile.readBytes()

            // Generate random salt and IV
            val random = SecureRandom()
            val salt = ByteArray(SALT_SIZE_BYTE)
            val iv = ByteArray(IV_SIZE_BYTE)
            random.nextBytes(salt)
            random.nextBytes(iv)

            val secretKey = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val encryptedBytes = cipher.doFinal(dbBytes)

            // Backup directory
            val backupDir = File(context.getExternalFilesDir("backups") ?: context.filesDir, "encrypted_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "finance_db_backup_$timestamp.enc")

            FileOutputStream(backupFile).use { fos ->
                fos.write(salt)
                fos.write(iv)
                fos.write(encryptedBytes)
            }

            val metadata = BackupMetadata(
                fileName = backupFile.name,
                filePath = backupFile.absolutePath,
                sizeBytes = backupFile.length(),
                timestamp = timestamp
            )

            Result.success(metadata)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun decryptAndRestoreDatabase(
        context: Context,
        backupFile: File,
        passphrase: String,
        targetDbName: String = "finance_tracker_db"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Backup file does not exist"))
            }

            val fileBytes = backupFile.readBytes()
            val minHeaderSize = SALT_SIZE_BYTE + IV_SIZE_BYTE
            if (fileBytes.size <= minHeaderSize) {
                return@withContext Result.failure(IllegalArgumentException("Invalid encrypted backup file structure"))
            }

            val salt = fileBytes.copyOfRange(0, SALT_SIZE_BYTE)
            val iv = fileBytes.copyOfRange(SALT_SIZE_BYTE, minHeaderSize)
            val encryptedBytes = fileBytes.copyOfRange(minHeaderSize, fileBytes.size)

            val secretKey = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedDbBytes = cipher.doFinal(encryptedBytes)

            // Close existing DB connections if possible before overwrite
            val targetDbFile = context.getDatabasePath(targetDbName)
            if (targetDbFile.exists()) {
                targetDbFile.delete()
            }

            // Write decrypted database file back
            FileOutputStream(targetDbFile).use { fos ->
                fos.write(decryptedDbBytes)
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun listBackups(context: Context): List<BackupMetadata> {
        val backupDir = File(context.getExternalFilesDir("backups") ?: context.filesDir, "encrypted_backups")
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles { _, name -> name.endsWith(".enc") }
            ?.map {
                BackupMetadata(
                    fileName = it.name,
                    filePath = it.absolutePath,
                    sizeBytes = it.length(),
                    timestamp = it.lastModified()
                )
            }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, KEY_GEN_ITERATIONS, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
