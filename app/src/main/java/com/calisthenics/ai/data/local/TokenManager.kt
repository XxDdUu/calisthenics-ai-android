package com.calisthenics.ai.data.local

import android.content.Context
import android.util.Base64
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    open fun saveAuthToken(accessToken: String, refreshToken: String, userId: Long? = null) {
        val resolvedUserId = userId ?: getUserId() ?: NO_USER_ID
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
            .putLong(KEY_USER_ID, resolvedUserId)
            .apply()
    }

    open fun getAccessToken(): String? = decrypt(preferences.getString(KEY_ACCESS_TOKEN, null))
    open fun getRefreshToken(): String? = decrypt(preferences.getString(KEY_REFRESH_TOKEN, null))
    open fun getUserId(): Long? = preferences.getLong(KEY_USER_ID, NO_USER_ID).takeIf { it != NO_USER_ID }
    open fun clear() = preferences.edit().clear().apply()

    open fun hasValidSession(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank() && !isRefreshTokenExpired()
    }

    open fun isAccessTokenExpired(): Boolean {
        return isTokenExpired(getAccessToken())
    }

    open fun isRefreshTokenExpired(): Boolean {
        return isTokenExpired(getRefreshToken())
    }

    open fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return true
            val payloadBytes = decodeBase64UrlSafe(parts[1])
            val payloadString = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadString).asJsonObject
            if (!jsonObject.has("exp")) return false
            val expSeconds = jsonObject.get("exp").asLong
            val currentSeconds = System.currentTimeMillis() / 1000
            currentSeconds >= expSeconds
        } catch (e: Throwable) {
            true
        }
    }

    private fun decodeBase64UrlSafe(encoded: String): ByteArray {
        return try {
            Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Throwable) {
            // Fallback for JVM test environments where android.util.Base64 is stubbed
            java.util.Base64.getUrlDecoder().decode(encoded)
        }
    }

    private fun encryptionKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
        return "$iv.$payload"
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val parts = encoded.split('.', limit = 2)
            if (parts.size != 2) return null

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val payload = Base64.decode(parts[1], Base64.NO_WRAP)
            if (iv.isEmpty() || payload.isEmpty()) return null

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val PREF_NAME = "calisthenics_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val NO_USER_ID = -1L
        private const val KEY_ALIAS = "calisthenics_auth_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
