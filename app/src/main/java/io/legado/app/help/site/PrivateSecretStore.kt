package io.legado.app.help.site

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import splitties.init.appCtx
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small local-only secret store for private-site session material.
 *
 * Android 23+ uses an AES/GCM key kept in AndroidKeyStore. API 21-22 fall back to the App's
 * private SharedPreferences because the modern symmetric AndroidKeyStore API is unavailable.
 */
object PrivateSecretStore {

    private const val PREFS = "penrix_private_site_secrets"
    private const val KEY_ALIAS = "penrix_private_site_session_key_v1"
    private const val ENCRYPTED_PREFIX = "gcm:"
    private const val PLAIN_PREFIX = "plain:"

    private val prefs by lazy {
        appCtx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String) {
        val stored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            encrypt(value)
        } else {
            PLAIN_PREFIX + Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
        prefs.edit().putString(key, stored).apply()
    }

    fun getString(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching {
            when {
                stored.startsWith(ENCRYPTED_PREFIX) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    decrypt(stored.removePrefix(ENCRYPTED_PREFIX))
                }
                stored.startsWith(PLAIN_PREFIX) -> {
                    String(
                        Base64.decode(stored.removePrefix(PLAIN_PREFIX), Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                }
                else -> null
            }
        }.getOrNull()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(
            cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        return "$ENCRYPTED_PREFIX$iv:$encrypted"
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun decrypt(payload: String): String {
        val separator = payload.indexOf(':')
        require(separator > 0) { "Invalid encrypted payload" }
        val iv = Base64.decode(payload.substring(0, separator), Base64.NO_WRAP)
        val encrypted = Base64.decode(payload.substring(separator + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }
}
