package com.example.crypto

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String,
    val iv: String,
    val encryptedKeySender: String,
    val encryptedKeyRecipient: String
)

object CryptoManager {
    private const val PREFS_NAME = "halloapp_crypto_prefs"
    private const val KEY_PUBLIC = "e2ee_public_key"
    private const val KEY_PRIVATE = "e2ee_private_key"

    private const val RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    /**
     * Get or generate RSA KeyPair for local device
     */
    fun getOrCreateKeyPair(context: Context): KeyPair {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pubStr = prefs.getString(KEY_PUBLIC, null)
        val privStr = prefs.getString(KEY_PRIVATE, null)

        if (pubStr != null && privStr != null) {
            try {
                val keyFactory = KeyFactory.getInstance("RSA")
                val pubBytes = Base64.decode(pubStr, Base64.NO_WRAP)
                val privBytes = Base64.decode(privStr, Base64.NO_WRAP)
                val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
                val privKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                return KeyPair(pubKey, privKey)
            } catch (e: Exception) {
                // If corrupted, re-generate below
            }
        }

        // Generate new 2048-bit RSA KeyPair
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val pubEncoded = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val privEncoded = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PUBLIC, pubEncoded)
            .putString(KEY_PRIVATE, privEncoded)
            .apply()

        return keyPair
    }

    fun getPublicKeyString(keyPair: KeyPair): String {
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    fun parsePublicKey(base64PublicKey: String): PublicKey {
        val bytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    /**
     * Encrypt message using hybrid AES-256-GCM + RSA OAEP.
     * The AES key is encrypted for both the sender and recipient so both can read it.
     */
    fun encryptMessage(
        plaintext: String,
        senderPublicKey: PublicKey,
        recipientPublicKey: PublicKey
    ): EncryptedPayload {
        // 1. Generate AES-256 key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val aesKey: SecretKey = keyGen.generateKey()

        // 2. Encrypt plaintext with AES-GCM
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        val aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val ciphertextBytes = aesCipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 3. Encrypt AES key with recipient's RSA public key
        val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        val encKeyRecipientBytes = rsaCipher.doFinal(aesKey.encoded)

        // 4. Encrypt AES key with sender's RSA public key (for sender's own message history)
        rsaCipher.init(Cipher.ENCRYPT_MODE, senderPublicKey)
        val encKeySenderBytes = rsaCipher.doFinal(aesKey.encoded)

        return EncryptedPayload(
            ciphertext = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            encryptedKeySender = Base64.encodeToString(encKeySenderBytes, Base64.NO_WRAP),
            encryptedKeyRecipient = Base64.encodeToString(encKeyRecipientBytes, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypt message using user's private RSA key and the appropriate encrypted key slot.
     */
    fun decryptMessage(
        ciphertextBase64: String,
        ivBase64: String,
        encryptedKeyBase64: String,
        privateKey: PrivateKey
    ): String {
        return try {
            // 1. Decrypt AES key with RSA private key
            val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
            val encKeyBytes = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
            val aesKeyBytes = rsaCipher.doFinal(encKeyBytes)
            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            // 2. Decrypt ciphertext with AES-GCM
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM)
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)

            val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val decryptedBytes = aesCipher.doFinal(ciphertextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "🔒 [Pesan Terenkripsi]"
        }
    }

    /**
     * Generates a 6-block Safety Number (Security code) to verify E2EE between two contacts.
     */
    fun computeSafetyNumber(publicKeyA: String, publicKeyB: String): String {
        val sortedKeys = listOf(publicKeyA, publicKeyB).sorted()
        val combined = sortedKeys[0] + ":" + sortedKeys[1]
        val digest = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray(Charsets.UTF_8))

        val blocks = mutableListOf<String>()
        for (i in 0 until 6) {
            val offset = i * 4
            val num = ((digest[offset].toInt() and 0xFF) shl 24) or
                    ((digest[offset + 1].toInt() and 0xFF) shl 16) or
                    ((digest[offset + 2].toInt() and 0xFF) shl 8) or
                    (digest[offset + 3].toInt() and 0xFF)
            val positiveNum = Math.abs(num % 100000)
            blocks.add(String.format("%05d", positiveNum))
        }
        return blocks.joinToString(" ")
    }
}
