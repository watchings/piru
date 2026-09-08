package app.piru.android

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object EncryptedBackup {
    private const val ITERATIONS = 250_000
    private const val KEY_BITS = 256

    fun encrypt(payload: ByteArray, passphrase: CharArray): String {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(payload)
        return JSONObject()
            .put("version", 1)
            .put("algorithm", "PBKDF2-SHA-256/AES-256-GCM")
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .toString()
    }

    fun decrypt(envelope: String, passphrase: CharArray): ByteArray {
        val json = JSONObject(envelope)
        require(json.optInt("version") == 1)
        require(json.optString("algorithm") == "PBKDF2-SHA-256/AES-256-GCM")
        val salt = Base64.decode(json.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(json.getString("iv"), Base64.DEFAULT)
        val ciphertext = Base64.decode(json.getString("ciphertext"), Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun key(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }
}
